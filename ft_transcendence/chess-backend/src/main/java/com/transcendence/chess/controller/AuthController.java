//Agrupa los endpoints HTTP relacionados con la autenticación y las cuentas de usuario.
package com.transcendence.chess.controller;

//Importa una lista para devolver varias amistades o perfiles en una respuesta.
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.transcendence.chess.dto.AccountUpdateRequest;
import com.transcendence.chess.dto.AddFriendRequest;
import com.transcendence.chess.dto.AuthRequest;
import com.transcendence.chess.dto.AuthResponse;
import com.transcendence.chess.dto.DeleteAccountRequest;
import com.transcendence.chess.dto.FriendView;
import com.transcendence.chess.dto.PublicProfileView;
import com.transcendence.chess.model.User;
import com.transcendence.chess.service.AuthService;

import jakarta.validation.Valid;

//Registra esta clase como controlador REST administrado por Spring.
@RestController
//Añade /api/auth delante de las rutas definidas en los métodos.
@RequestMapping("/api/auth")
//Declara los endpoints HTTP de autenticación y gestión de relaciones de usuario.
public class AuthController {

    //Referencia al componente que contiene la lógica de negocio de estos endpoints.
    private final AuthService authService;

    //Spring inyecta aquí el servicio al crear el controlador.
    public AuthController(AuthService authService) {
        //Conserva la dependencia para usarla desde los métodos del controlador.
        this.authService = authService;
    }

    //Atiende POST /api/auth/register y valida los campos del cuerpo JSON antes de delegar.
    @PostMapping("/register")
    //Recibe los datos de registro y devuelve la respuesta de autenticación del servicio.
    public AuthResponse register(@Valid @RequestBody AuthRequest request) {
        //Deja que AuthService compruebe y procese el registro.
        return authService.register(request);
    }

    //Atiende POST /api/auth/login para iniciar sesión.
    @PostMapping("/login")
    //Recibe y valida el cuerpo con las credenciales de acceso.
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        //Delega la verificación de credenciales y la creación de respuesta.
        return authService.login(request);
    }

    //Atiende POST /api/auth/logout para cerrar la sesión asociada a la cabecera recibida.
    @PostMapping("/logout")
    //Indica que la operación correcta responde con HTTP 204 y sin cuerpo.
    @ResponseStatus(HttpStatus.NO_CONTENT)
    //Exige la cabecera Authorization, que contiene las credenciales de la sesión.
    public void logout(@RequestHeader("Authorization") String authorization) {
        //Comprueba que la cabecera permite identificar a un usuario válido.
        authService.requireUserId(authorization);
        //Pide al servicio invalidar o cerrar la sesión correspondiente.
        authService.logout(authorization);
    }

    //Atiende PUT /api/auth/account para actualizar los datos de la cuenta autenticada.
    @PutMapping("/account")
    //Recibe la cabecera de sesión y los nuevos valores validados del cuerpo JSON.
    public User updateAccount(@RequestHeader("Authorization") String authorization,
                              @Valid @RequestBody AccountUpdateRequest request) {
        //Resuelve el usuario de la cabecera y pasa su identificador y los cambios al servicio.
        return authService.updateAccount(authService.requireUserId(authorization), request);
    }

    //Atiende DELETE /api/auth/account para solicitar la eliminación de la cuenta autenticada.
    @DeleteMapping("/account")
    //Devuelve HTTP 204 sin cuerpo cuando la cuenta se elimina correctamente.
    @ResponseStatus(HttpStatus.NO_CONTENT)
    //Recibe la autorización y la contraseña actual como confirmación de la operación.
    public void deleteAccount(@RequestHeader("Authorization") String authorization,
                              @Valid @RequestBody DeleteAccountRequest request) {
        //Identifica al usuario y delega la eliminación junto con su contraseña de confirmación.
        authService.deleteAccount(authService.requireUserId(authorization), request.getCurrentPassword());
    }

    //Atiende GET /api/auth/friends para listar las amistades del usuario autenticado.
    @GetMapping("/friends")
    //Lee la autorización y devuelve una lista de vistas de amistad.
    public List<FriendView> getFriends(@RequestHeader("Authorization") String authorization) {
        //Resuelve el usuario y solicita al servicio sus amistades.
        return authService.getFriends(authService.requireUserId(authorization));
    }

    //Atiende GET /api/auth/friends/search para buscar usuarios con los que iniciar una amistad.
    @GetMapping("/friends/search")
    //Recibe la sesión y el texto de búsqueda en el parámetro query de la URL.
    public List<FriendView> searchUsers(@RequestHeader("Authorization") String authorization,
                                        @RequestParam String query) {
        //Resuelve quién busca y delega la búsqueda del texto recibido.
        return authService.searchUsers(authService.requireUserId(authorization), query);
    }

    //Atiende GET /api/auth/users/{userId} para consultar un perfil público por su identificador.
    @GetMapping("/users/{userId}")
    //Extrae userId de la ruta y exige la cabecera de autorización.
    public PublicProfileView getPublicProfile(@RequestHeader("Authorization") String authorization, 
            @PathVariable Long userId) {
        //Verifica que la petición corresponde a un usuario autenticado.
        authService.requireUserId(authorization);
        //Obtiene y devuelve la vista pública del usuario solicitado.
        return authService.getPublicProfile(userId);
    }

    //Atiende POST /api/auth/friends para añadir una amistad.
    @PostMapping("/friends")
    //Recibe la autorización y el nombre del usuario que se quiere añadir.
    public FriendView addFriend(@RequestHeader("Authorization") String authorization,
                                @RequestBody AddFriendRequest request) {
        //Identifica al solicitante y delega la operación con el nombre recibido.
        return authService.addFriend(authService.requireUserId(authorization), request.getUsername());
    }

    //Atiende DELETE /api/auth/friends/{friendId} para eliminar una amistad concreta.
    @DeleteMapping("/friends/{friendId}")
    //Devuelve HTTP 204 sin cuerpo cuando la relación se elimina correctamente.
    @ResponseStatus(HttpStatus.NO_CONTENT)
    //Extrae la autorización de la cabecera y el identificador de amistad de la ruta.
    public void removeFriend(@RequestHeader("Authorization") String authorization,
                             @PathVariable Long friendId) {
        //Resuelve al solicitante y delega la eliminación de la amistad indicada.
        authService.removeFriend(authService.requireUserId(authorization), friendId);
    }
}
