package es.gav.pujador.servicioweb;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import es.gav.controladores.FieldDataFieldEstadoFacade;
import es.gav.controladores.FieldDataFieldMaximoPujadorFacade;
import es.gav.controladores.FieldDataFieldMaximoPujadorIdFacade;
import es.gav.controladores.FieldDataFieldPvpFacade;
import es.gav.controladores.NodeFacade;
import es.gav.controladores.UsersFacade;
import es.gav.entidades.FieldDataFieldEstado;
import es.gav.entidades.FieldDataFieldEstadoPK;
import es.gav.entidades.FieldDataFieldMaximoPujador;
import es.gav.entidades.FieldDataFieldMaximoPujadorId;
import es.gav.entidades.FieldDataFieldMaximoPujadorIdPK;
import es.gav.entidades.FieldDataFieldMaximoPujadorPK;
import es.gav.entidades.FieldDataFieldPvp;
import es.gav.entidades.FieldDataFieldPvpPK;
import es.gav.entidades.Node;
import es.gav.entidades.Users;
import es.gav.excepciones.PujadorExcepcion;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * REST Web Service
 *
 * @author Enrique Ríos Santos
 */
@Path("pujador")
public class ServicioWeb {

    //Controladores
    @EJB
    private FieldDataFieldPvpFacade pvpC;
    @EJB
    private FieldDataFieldMaximoPujadorFacade maximoPujadorC;
    @EJB
    private NodeFacade nodoC;
    @EJB
    private FieldDataFieldEstadoFacade estadoC;
    @EJB
    private FieldDataFieldMaximoPujadorIdFacade maximoPujadorIdC;
    @EJB
    private UsersFacade usuarioC;

    @Context
    private UriInfo context;

    /**
     * Creates a new instance of ServicioWeb
     */
    public ServicioWeb() {
    }

    /**
     * Función para confirmar la conexión con el servicio web es correcta
     *
     * @return
     */
    @GET
    @Path("test")
    @Produces(MediaType.TEXT_PLAIN)
    public String test() {
        return "WS ok";
    }

    /**
     * Realiza una puja de un usuario sobre una obra
     *
     * @param peticionPuja - JSON que contiene el id del usuario pujador, el id de la obra y la cantidad pujada
     * @return - Devuelve un JSON con dos elementos, 
     *          Estado: OK en caso de éxito; KO en caso de error
     *          Mensaje: Texto identificativo del resultado obtenido
     */
    @POST
    @Path("pujar")
    @Consumes("application/json")
    @Produces("application/json")
    public String pujar(String peticionPuja) {
        Gson gson = new Gson();
        String respuesta = new String();
        Map<String, String> mapaRespuesta = new HashMap<>();
        mapaRespuesta.put("Resultado", "KO");
        try {
            Type tipoMapa = new TypeToken<Map<String, Object>>() {
            }.getType();
            //Recoger datos de la petición
            LinkedTreeMap<String, Object> mapaPeticion = gson.fromJson(peticionPuja, tipoMapa);
            Integer idUsuario = ((Double) mapaPeticion.get("usuario")).intValue();
            Integer idObra = ((Double) mapaPeticion.get("obra")).intValue();
            Double cantidadPuja = (Double) mapaPeticion.get("cantidad");
            respuesta = pujarObra(idUsuario, idObra, cantidadPuja);
            mapaRespuesta.put("Resultado", "OK");
        } catch (JsonParseException jpe) {
            Logger.getLogger(ServicioWeb.class.getName()).log(Level.SEVERE, null, jpe);
            respuesta = "Error al recibir los datos: " + jpe.getLocalizedMessage();
        } catch (ClassCastException cce) {
            Logger.getLogger(ServicioWeb.class.getName()).log(Level.SEVERE, null, cce);
            respuesta = "Formato de datos incorrecto: " + cce.getLocalizedMessage();
        } catch (PujadorExcepcion ex) {
            Logger.getLogger(ServicioWeb.class.getName()).log(Level.SEVERE, null, ex);
            respuesta = ex.getMessage();
        } catch (Exception e) {
            respuesta = "Error en el servidor";
        } finally {
            mapaRespuesta.put("Mensaje", respuesta);
        }

        return gson.toJson(mapaRespuesta);
    }

    //Métodos auxiliares
    /**
     * Realiza una puja sobre una obra de un usuario
     *
     * @param idUsuario - ID del usuario que realiza la puja
     * @param idObra - ID de la obra a la que realiza la obra
     * @param cantidadPuja - Cantidad (en euros) que puja el usuario
     */
    private String pujarObra(Integer idUsuario, Integer idObra, Double cantidadPuja)
            throws PujadorExcepcion {
        //Recupera información de la obra y el usuario de la base de datos
        Node nodo = nodoC.find(idObra);
        Users usuario = usuarioC.find(idUsuario);

        if (nodo == null) {
            throw new PujadorExcepcion("Obra no encontrada");
        }

        if (usuario == null) {
            throw new PujadorExcepcion("Usuario no encontrado");
        }

        //Precio de venta al público
        FieldDataFieldPvpPK pvpPK = new FieldDataFieldPvpPK("node", Short.parseShort("0"), idObra, "und", 0);
        FieldDataFieldPvp pvp = pvpC.find(pvpPK);
        //Comprobaciones
        //Puja inferior a la actual
        if (cantidadPuja < pvp.getFieldPvpValue().doubleValue()) {
            throw new PujadorExcepcion("Puja inferior al precio actual");
        }
        //Está en subasta
        FieldDataFieldEstadoPK estadoPK = new FieldDataFieldEstadoPK("node", Short.parseShort("0"), idObra, "und", 0);
        FieldDataFieldEstado estado = estadoC.find(estadoPK);
        if (estado == null || !estado.getFieldEstadoValue().equals("subasta")) {
            throw new PujadorExcepcion("La obra no tiene el estado de subasta");
        }
        //Si el usuario que puja es ya el máximo pujador
        FieldDataFieldMaximoPujadorIdPK maximoPujadorIdPK = new FieldDataFieldMaximoPujadorIdPK("node", Short.parseShort("0"), idObra, "und", 0);
        FieldDataFieldMaximoPujadorId maximoPujadorId = maximoPujadorIdC.find(maximoPujadorIdPK);
        if (maximoPujadorId == null) {
            throw new PujadorExcepcion("Error al recuperar al máximo pujador");
        }
        if (maximoPujadorId.getFieldMaximoPujadorIdValue().equals(idUsuario)) {
            throw new PujadorExcepcion("El usuario ya es el máximo pujador");
        }

        //Puja
        //Actualiza ID del máximo pujador
        maximoPujadorId.setFieldMaximoPujadorIdValue(idUsuario);
        //Actualiza el nombre del máximo pujador
        FieldDataFieldMaximoPujadorPK maximoPujadorPK = new FieldDataFieldMaximoPujadorPK("node", Short.parseShort("0"), idObra, "und", 0);
        FieldDataFieldMaximoPujador maximoPujador = maximoPujadorC.find(maximoPujadorPK);
        maximoPujador.setFieldMaximoPujadorValue(usuario.getName());
        //Actualiza el valor pujado
        pvp.setFieldPvpValue(BigDecimal.valueOf(cantidadPuja));
        //Reflejamos los cambios en la base de datos
        maximoPujadorC.edit(maximoPujador);
        maximoPujadorIdC.edit(maximoPujadorId);
        pvpC.edit(pvp);

        return "Puja realizada correctamente";
    }
}
