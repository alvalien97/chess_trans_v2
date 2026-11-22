//Paquete de modelos usados para representar datos de la aplicación.
package com.transcendence.chess.model;

//Objeto de transporte de un mensaje enviado al chat en tiempo real.
public class ChatMessage {
    //Nombre que se muestra como remitente.
    private String sender;
    //Texto del mensaje enviado.
    private String content;

    //Getters y Setters
    //Devuelve el remitente.
    public String getSender() { return sender; }
    //Actualiza el remitente, normalmente con el usuario validado por el servidor.
    public void setSender(String sender) { this.sender = sender; }
    //Devuelve el texto del mensaje.
    public String getContent() { return content; }
    //Guarda el texto del mensaje.
    public void setContent(String content) { this.content = content; }
}
