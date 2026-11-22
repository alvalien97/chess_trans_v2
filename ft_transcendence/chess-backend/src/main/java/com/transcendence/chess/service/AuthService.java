//Paquete que contiene la lógica de negocio del backend.
package com.transcendence.chess.service;

//Colecciones usadas para devolver resultados y gestionar sesiones en memoria.
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.transcendence.chess.dto.AccountUpdateRequest;
import com.transcendence.chess.dto.AuthRequest;
import com.transcendence.chess.dto.AuthResponse;
import com.transcendence.chess.dto.FriendView;
import com.transcendence.chess.dto.PublicProfileView;
import com.transcendence.chess.model.Friendship;
import com.transcendence.chess.model.User;
import com.transcendence.chess.repository.FriendshipRepository;
import com.transcendence.chess.repository.GameRepository;
import com.transcendence.chess.repository.UserRepository;

//Implementa registro, inicio/cierre de sesión, perfiles y amistades.
@Service
public class AuthService {

    //Acceso persistente a cuentas.
    private final UserRepository userRepository;
    //Permite impedir el borrado mientras haya partidas activas.
    private final GameRepository gameRepository;
    //Acceso persistente a las relaciones de amistad.
    private final FriendshipRepository friendshipRepository;
    //Codificador que evita almacenar contraseñas en texto plano.
    private final PasswordEncoder passwordEncoder;
    //Permite sacar de la cola a una cuenta eliminada.
    private final MatchmakingService matchmakingService;
    //Mantiene actualizado el estado de conexión de la cuenta.
    private final PresenceService presenceService;
    //Mapa concurrente que relaciona tokens aleatorios con ids de usuario.
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();

    //Constructor de inyección de dependencias de Spring.
    public AuthService(UserRepository userRepository, GameRepository gameRepository,
                       FriendshipRepository friendshipRepository,
                       PasswordEncoder passwordEncoder,
                       MatchmakingService matchmakingService,
                       PresenceService presenceService) {
        //Guarda el repositorio de usuarios.
        this.userRepository = userRepository;
        //Guarda el repositorio de partidas.
        this.gameRepository = gameRepository;
        //Guarda el repositorio de amistades.
        this.friendshipRepository = friendshipRepository;
        //Guarda el codificador de contraseñas.
        this.passwordEncoder = passwordEncoder;
        //Guarda el servicio de emparejamiento.
        this.matchmakingService = matchmakingService;
        //Guarda el servicio de presencia.
        this.presenceService = presenceService;
    }

    //Valida y crea una cuenta nueva, y devuelve un token de sesión.
    public AuthResponse register(AuthRequest request) {
        //Rechaza un nombre de usuario ausente o compuesto por espacios.
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }
        //Evita registrar un correo que ya pertenezca a otra cuenta.
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Ese email ya está registrado.");
        }
        //Evita registrar un nombre de usuario duplicado.
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Ese nombre de usuario ya existe.");
        }

        //Crea una entidad de usuario nueva.
        User user = new User();
        //Elimina espacios externos del nombre antes de guardarlo.
        user.setUsername(request.getUsername().trim());
        //Normaliza el correo para que las búsquedas no dependan de espacios o mayúsculas.
        user.setEmail(request.getEmail().trim().toLowerCase());
        //Codifica la contraseña antes de guardarla en la base de datos.
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        //Persiste la cuenta; JPA asigna su id.
        userRepository.save(user);
        //Crea una sesión y arma la respuesta de registro.
        return issueToken(user);
    }

    //Verifica las credenciales de una cuenta existente y devuelve una sesión.
    public AuthResponse login(AuthRequest request) {
        //Normaliza el correo y busca la cuenta; el mismo error evita revelar si existe.
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Email o contraseña incorrectos."));
        //Compara la contraseña proporcionada con el hash almacenado.
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Email o contraseña incorrectos.");
        }
        //Crea un token de acceso para esta cuenta.
        return issueToken(user);
    }

    //Actualiza uno o varios datos de perfil o credenciales dentro de una transacción.
    @Transactional
    public User updateAccount(Long userId, AccountUpdateRequest request) {
        //Carga al usuario o informa que no existe.
        User user = requireUser(userId);
        //Guarda localmente la contraseña nueva para reutilizarla en las validaciones.
        String newPassword = request.getNewPassword();
        //Cambiar nombre o contraseña requiere confirmar primero la contraseña actual.
        if (request.getUsername() != null || (newPassword != null && !newPassword.isBlank())) {
            requireCurrentPassword(user, request.getCurrentPassword());
        }

        //Actualiza el nombre solo si el cliente envió uno.
        if (request.getUsername() != null) {
            //Normaliza el nombre eliminando espacios externos.
            String username = request.getUsername().trim();
            //Valida la longitud después de normalizarlo.
            if (username.length() < 3 || username.length() > 32) {
                throw new IllegalArgumentException("El nombre debe tener entre 3 y 32 caracteres.");
            }
            //Comprueba duplicados solo si el usuario realmente quiere cambiar el nombre.
            if (!username.equals(user.getUsername()) && userRepository.existsByUsername(username)) {
                throw new IllegalArgumentException("Ese nombre de usuario ya existe.");
            }
            //Actualiza el campo de nombre de usuario.
            user.setUsername(username);
        }

        //Cambia la contraseña solo si el cliente proporcionó una no vacía.
        if (newPassword != null && !newPassword.isBlank()) {
            //Verifica la longitud aceptada por el backend.
            if (newPassword.length() < 8 || newPassword.length() > 72) {
                throw new IllegalArgumentException("La nueva contraseña debe tener entre 8 y 72 caracteres.");
            }
            //Guarda únicamente el hash de la contraseña nueva.
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }

        //Actualiza el avatar solo si el campo se incluyó en la petición.
        if (request.getAvatarUrl() != null) {
            //Normaliza la URL recibida.
            String avatarUrl = request.getAvatarUrl().trim();
            //Una cadena vacía elimina el avatar actual.
            if (avatarUrl.isBlank()) {
                user.setAvatarUrl(null);
            //Acepta imágenes data:image o enlaces HTTP(S), limitando el tamaño de la cadena.
            } else if (avatarUrl.length() > 5_000_000 || (!avatarUrl.startsWith("data:image/")
                    && !avatarUrl.startsWith("http://") && !avatarUrl.startsWith("https://"))) {
                throw new IllegalArgumentException("La imagen de perfil no es válida.");
            } else {
                //Guarda el avatar cuando supera las comprobaciones anteriores.
                user.setAvatarUrl(avatarUrl);
            }
        }
        //Persiste los cambios y devuelve la entidad actualizada.
        return userRepository.save(user);
    }

    //Consulta la lista de amistades del usuario, solo lectura.
    @Transactional(readOnly = true)
    public List<FriendView> getFriends(Long userId) {
        //Comprueba que la cuenta solicitante exista.
        requireUser(userId);
        //Busca sus relaciones, convierte cada amigo a una vista pública y devuelve la lista.
        return friendshipRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(friendship -> toFriendView(friendship.getFriend()))
                .toList();
    }

    //Busca hasta diez usuarios cuyo nombre contenga el texto indicado.
    @Transactional(readOnly = true)
    public List<FriendView> searchUsers(Long userId, String query) {
        //Rechaza búsquedas iniciadas desde una cuenta inexistente.
        requireUser(userId);
        //Convierte null en cadena vacía y elimina espacios externos.
        String cleanQuery = query == null ? "" : query.trim();
        //Evita consultas demasiado cortas.
        if (cleanQuery.length() < 2) {
            return List.of();
        }

        //Recopila ids de las amistades actuales para no mostrarlas en los resultados.
        Set<Long> existingFriendIds = friendshipRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(friendship -> friendship.getFriend().getId())
                .collect(Collectors.toSet());
            //Busca coincidencias excluyendo al solicitante, elimina amigos existentes y convierte a vistas.
        return userRepository.findTop10ByUsernameContainingIgnoreCaseAndIdNotOrderByUsernameAsc(cleanQuery, userId).stream()
                .filter(candidate -> !existingFriendIds.contains(candidate.getId()))
                .map(this::toFriendView)
                .toList();
    }

    //Devuelve una vista pública del usuario, incluyendo su estado de presencia.
    @Transactional(readOnly = true)
    public PublicProfileView getPublicProfile(Long userId) {
        //Busca la cuenta pedida.
        User user = requireUser(userId);
        //Construye una respuesta con campos públicos y consulta de presencia.
        return new PublicProfileView(user.getId(), user.getUsername(), user.getAvatarUrl(),
                user.getCreatedAt(), presenceService.isOnline(userId));
    }

    //Crea la relación de amistad en ambos sentidos si aún no existe.
    @Transactional
    public FriendView addFriend(Long userId, String username) {
        //Carga la cuenta que inicia la operación.
        User currentUser = requireUser(userId);
        //Normaliza el nombre recibido y tolera un valor null.
        String cleanUsername = username == null ? "" : username.trim();
        //Rechaza una búsqueda de amistad sin nombre.
        if (cleanUsername.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }

        //Busca por nombre ignorando mayúsculas y usa una búsqueda exacta como alternativa.
        User friend = userRepository.findByUsernameIgnoreCase(cleanUsername)
                .or(() -> userRepository.findByUsername(cleanUsername))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

            //No se permite crear una amistad consigo mismo.
        if (friend.getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("No puedes agregarte a ti mismo.");
        }

        //Crea la dirección solicitante->amigo si no existe.
        if (!friendshipRepository.existsByUserIdAndFriendId(currentUser.getId(), friend.getId())) {
            //Prepara la relación del usuario actual hacia el amigo.
            Friendship currentRelation = new Friendship();
            currentRelation.setUser(currentUser);
            currentRelation.setFriend(friend);
            //Persiste la primera dirección.
            friendshipRepository.save(currentRelation);

            //Prepara también la dirección inversa para que ambos vean la relación.
            Friendship reverseRelation = new Friendship();
            reverseRelation.setUser(friend);
            reverseRelation.setFriend(currentUser);
            //Evita duplicar la relación inversa si ya existía.
            if (!friendshipRepository.existsByUserIdAndFriendId(friend.getId(), currentUser.getId())) {
                friendshipRepository.save(reverseRelation);
            }
        }

        //Devuelve los datos visibles del usuario añadido.
        return toFriendView(friend);
    }

    //Elimina las dos direcciones de una relación de amistad.
    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        //Verifica que quien solicita eliminar la relación exista.
        requireUser(userId);
        //Borra la dirección desde el usuario hacia su amigo.
        friendshipRepository.deleteByUserIdAndFriendId(userId, friendId);
        //Borra la dirección inversa.
        friendshipRepository.deleteByUserIdAndFriendId(friendId, userId);
    }

    //Invalida el token asociado a la cabecera Authorization, si tiene formato Bearer.
    public void logout(String authorizationHeader) {
        //Comprueba que la cabecera no sea null y comience con el prefijo esperado.
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            //Extrae el token después de los siete caracteres de "Bearer ".
            String token = authorizationHeader.substring(7);
            //Quita la sesión; si existía, revoca también las conexiones WebSocket asociadas.
            if (sessions.remove(token) != null) {
                presenceService.revokeToken(token);
            }
        }
    }

    //Anonimiza la cuenta y limpia sus relaciones y sesiones tras verificar la contraseña.
    @Transactional
    public void deleteAccount(Long userId, String currentPassword) {
        //Carga al usuario a eliminar.
        User user = requireUser(userId);
        //Exige la contraseña actual como confirmación.
        requireCurrentPassword(user, currentPassword);
        //Obliga a terminar partidas activas antes de poder borrar la cuenta.
        if (!gameRepository.findActiveGamesByUser(userId).isEmpty()) {
            throw new IllegalArgumentException("Termina tus partidas activas antes de eliminar la cuenta.");
        }
        //Elimina al usuario de la cola de emparejamiento.
        matchmakingService.leaveQueue(userId);
        //Elimina las relaciones donde aparece como iniciador.
        friendshipRepository.deleteByUserId(userId);
        //Elimina las relaciones donde aparece como amigo.
        friendshipRepository.deleteByFriendId(userId);

        //Genera un sufijo único para liberar nombre y correo sin colisiones.
        String suffix = userId + "-" + UUID.randomUUID();
        //Sustituye el nombre por uno de cuenta eliminada.
        user.setUsername("deleted_" + suffix.substring(0, Math.min(suffix.length(), 24)));
        //Sustituye el correo por uno reservado que no puede ser usado para iniciar sesión.
        user.setEmail("deleted+" + suffix + "@deleted.invalid");
        //Sustituye la credencial por un hash aleatorio desconocido.
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        //Elimina la imagen de perfil.
        user.setAvatarUrl(null);
        //Persiste la anonimización de la cuenta.
        userRepository.save(user);
        //Quita de memoria todos los tokens asociados a este usuario.
        sessions.entrySet().removeIf(session -> session.getValue().equals(userId));
        //Cierra sus conexiones y publica que ya no está en línea.
        presenceService.forceOffline(userId);
    }

    //Extrae y valida el id de usuario desde una cabecera Authorization de tipo Bearer.
    public Long requireUserId(String authorizationHeader) {
        //Si falta la cabecera o el formato es incorrecto, responde con HTTP 401.
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticación requerida.");
        }

        //Busca el token en el mapa de sesiones activas.
        Long userId = sessions.get(authorizationHeader.substring(7));
        //Rechaza tokens desconocidos o sesiones asociadas a cuentas que ya no existen.
        if (userId == null || userRepository.findById(userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión no válida.");
        }
        //Devuelve el id autenticado para que el controlador no confíe en un id del cliente.
        return userId;
    }

    //Genera un token opaco, lo registra en memoria y crea la respuesta de autenticación.
    private AuthResponse issueToken(User user) {
        //UUID aleatorio para identificar esta sesión.
        String token = UUID.randomUUID().toString();
        //Asocia el token con el id del usuario.
        sessions.put(token, user.getId());
        //Devuelve el token y los datos del usuario al cliente.
        return new AuthResponse(token, user);
    }

    //Consulta si hay al menos una sesión WebSocket activa para este usuario.
    public boolean isUserOnline(Long userId) {
        return userId != null && presenceService.isOnline(userId);
    }

    //Comprueba si hay algún token de sesión activo asociado al id indicado.
    public boolean hasActiveSession(Long userId) {
        return userId != null && sessions.containsValue(userId);
    }

    //Convierte el usuario a los campos que se muestran en listas de amistades.
    private FriendView toFriendView(User user) {
        return new FriendView(user.getId(), user.getUsername(), user.getAvatarUrl(), isUserOnline(user.getId()));
    }

    //Busca un usuario o responde con HTTP 404 si no existe.
    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
    }

    //Comprueba la contraseña actual antes de permitir cambios sensibles.
    private void requireCurrentPassword(User user, String currentPassword) {
        //Rechaza una contraseña ausente o cuyo hash no coincida con el almacenado.
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }
    }
}
