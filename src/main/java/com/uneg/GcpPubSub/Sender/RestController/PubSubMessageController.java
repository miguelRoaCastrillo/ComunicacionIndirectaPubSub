package com.uneg.GcpPubSub.Sender.RestController;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.*;
import com.uneg.GcpPubSub.Sender.GcpPubSubSenderApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class PubSubMessageController {

    @Value("${ID_PROYECTO}")
    String idProyecto;

    /*
        @Autowired
        private GcpPubSubSenderApplication.PubsubOutboundGateway messagingGateway;
     */

    /* Intento fallido
        @PostMapping("/publishMessage")
        public ResponseEntity<?> publishMessage(@RequestParam("message") String message){
            messagingGateway.sendToPubSub(message);
            return new ResponseEntity<>(HttpStatus.OK);
        }
     */

    /**
     * Lista todos los temas de pub/Sub disponibles para la cuenta de gcloud
     * @return
     * @throws IOException
     */
    @GetMapping("/ListarTemas")
    public ArrayList<String> listarTemas() throws IOException {

        ArrayList<String> listaTemas = new ArrayList<>();

        //Para encontrar todos los temas de google cloud
        try (TopicAdminClient topicAdminClient = TopicAdminClient.create()) {
            ProjectName projectName = ProjectName.of(idProyecto);
            for (Topic topic : topicAdminClient.listTopics(projectName).iterateAll()) {
                System.out.println(topic.getName());
                listaTemas.add(topic.getName());
            }

            return listaTemas;
        }catch(Exception error){
            System.out.println("Existe un error al tomar la lista de temas: " + error.toString());
            return (ArrayList) null;
        }
    }

    /**
     * Crea un tema directamente desde la aplicacion como interfaz para gcloud
     * @param nombreTema
     * @return
     */
    @PostMapping("/CrearTema")
    public Boolean crearTema(@RequestParam("nombreTema") String nombreTema){
        try(
            TopicAdminClient topicAdminClient = TopicAdminClient.create()
        ){
            //Se quitan todos los espacios vacíos del String para evitar problemas
            String fixNombreTema = nombreTema.replaceAll("\\s","");

            //La cadena del nombre no puede tener la sub cadena "goog"
            if(fixNombreTema.contains("goog")){
                throw new Exception("El nombre del tema no puede contener goog");
            }

            //Se crea el proyecto
            TopicName topicName = TopicName.of(idProyecto, fixNombreTema);
            Topic topic = topicAdminClient.createTopic(topicName);
            System.out.println("Tema creado: " + topic.getName());

            return true;

        }catch(Exception error){
            System.out.println("Existe un error al momento de crear el tema: " + error.toString());
            return false;
        }
    }

    /**
     * Borra un tema desde el programa para gcloud
     *
     * @param nombreTema
     * @return
     */
    @PostMapping("/BorrarTema")
    public Boolean borrarTema(@RequestParam("nombreTema")String nombreTema){
        try(
            TopicAdminClient topicAdminClient = TopicAdminClient.create()
        ){
            //Quita los espacios vacíos en la cadena de caracteres
            String fixNombreTema = nombreTema.replaceAll("\\s","");

            TopicName topicName = TopicName.of(idProyecto, fixNombreTema);

            topicAdminClient.deleteTopic(topicName);
            System.out.println("Tema borrado.");

            return true;

        }catch(Exception error){
            System.out.println("Existe un error para eliminar el tema indicado: " + error.toString());
            return false;
        }
    }

    /**
     * Lista todas las suscripciones del usuario de google cloud
     * @param nombreTema
     * @return
     */
    @PostMapping("/ListarSuscripciones")
    public ArrayList<String> listarSuscripciones(@RequestParam("nombreTema") String nombreTema){
        try(
            TopicAdminClient topicAdminClient = TopicAdminClient.create()
        ){
            String fixNombreTema = nombreTema.replaceAll("\\s","");
            TopicName topicName = TopicName.of(idProyecto, fixNombreTema);

            ArrayList<String> listaSuscripciones = new ArrayList<>();

            for (String subscription : topicAdminClient.listTopicSubscriptions(topicName).iterateAll()) {
                listaSuscripciones.add(subscription);
                System.out.println(subscription);
            }

            System.out.println("Ya deben de estar listadas todas las suscripciones.");

            if(listaSuscripciones.isEmpty()){
                throw new Exception("La lista de suscripciones está vacía");
            }

            return listaSuscripciones;

        }catch(Exception error){
            System.out.println("Existe un error al momento de listar las suscripciones: " + error.toString());
            return (ArrayList<String>) null;
        }
    }

    /**
     * Elimina las suscripciones de google cloud
     * @param nombreSuscripcion
     * @return
     */
    @PostMapping("/EliminarSuscripcion")
    public Boolean borrarSuscripcion(@RequestParam("nombreSuscripcion") String nombreSuscripcion){
        try(
            SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()
        ){
            String fixNombreSuscripcion = nombreSuscripcion.replaceAll("\\s","");

            ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(idProyecto, fixNombreSuscripcion);
            System.out.println("Suscripción borrada.");

            return true;

        }catch(Exception error){
            System.out.println("Existe un error al eliminar la suscripcion: " + error.toString());
            return false;
        }
    }

    /**
     * Para enviar un mensaje a un tema en especifico
     * @param idTema
     * @param mensaje
     * @return
     * @throws Exception
     */
    @PostMapping("/EnviarMensaje")
    public Boolean EnviarMensaje(
            @RequestParam("idTema") String idTema,
            @RequestParam("mensaje") String mensaje
    ) throws Exception{

        TopicName topicName = null;
        Publisher publisher = null;

        try{
            topicName = TopicName.of(idProyecto, idTema);
            publisher = Publisher.newBuilder(topicName).build();


            ByteString data = ByteString.copyFromUtf8(mensaje);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

            //Una vez publicado, retorna un id de mensaje asignado al servidor
            ApiFuture<String> future = publisher.publish(pubsubMessage);

            //Añade un callback asincrono para manejar el proceso exitoso o un error
            ApiFutures.addCallback(
                future,
                new ApiFutureCallback<String>() {

                    @Override
                    public void onFailure(Throwable throwable) {
                        if (throwable instanceof ApiException) {
                            ApiException apiException = ((ApiException) throwable);
                            System.out.println(apiException.getStatusCode().getCode());
                            System.out.println(apiException.isRetryable());
                        }
                        System.out.println("Error al publicar el siguiente mensaje : " + mensaje);
                    }

                    @Override
                    public void onSuccess(String idMensaje) {
                        //Una vez publicado, retorna los ids del mensaje asignado por servidor
                        System.out.println("Id de mensaje publicado: " + idMensaje);
                    }
                },
                MoreExecutors.directExecutor()
            );

            //Si se envia el mensaje correctamente
            return true;

        }catch(Exception error){
            System.out.println("Existe un error al momento de enviar el mensaje: " + error.toString());
            return false;
        } finally {
            if (publisher != null) {
                //Cuando haya terminado de funcionar el publicador, lo apaga para liberar recursos
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        }
    }
}