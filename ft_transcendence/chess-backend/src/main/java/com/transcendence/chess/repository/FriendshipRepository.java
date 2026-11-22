//Paquete que agrupa interfaces de acceso a la base de datos.
package com.transcendence.chess.repository;

//Colección usada para devolver varias amistades.
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transcendence.chess.model.Friendship;

//Spring Data crea consultas y borrados leyendo los nombres de método.
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    //Lista las relaciones iniciadas por el usuario y las ordena por id.
    List<Friendship> findByUserIdOrderByIdAsc(Long userId);
    //Comprueba si existe la relación indicada.
    boolean existsByUserIdAndFriendId(Long userId, Long friendId);
    //Elimina relaciones donde el usuario es quien las inició.
    void deleteByUserId(Long userId);
    //Elimina relaciones donde el usuario figura como amigo.
    void deleteByFriendId(Long friendId);
    //Elimina una relación concreta entre los dos identificadores.
    void deleteByUserIdAndFriendId(Long userId, Long friendId);
}
