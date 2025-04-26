package lsi.ubu.servicios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lsi.ubu.excepciones.AlquilerCochesException;
import lsi.ubu.util.PoolDeConexiones;
import lsi.ubu.util.exceptions.SGBDError;
import lsi.ubu.util.exceptions.oracle.OracleSGBDErrorUtil;


public class ServicioImpl implements Servicio {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServicioImpl.class);

	private static final int DIAS_DE_ALQUILER = 4;

	public void alquilar(String nifCliente, String matricula, Date fechaIni, Date fechaFin) throws SQLException {
		
		PoolDeConexiones pool = PoolDeConexiones.getInstance();

		Connection con = null;
		PreparedStatement selDisponible = null;
		PreparedStatement insReserva = null;
		PreparedStatement insFactura = null;
		ResultSet cursor = null;

		/*
		 * El calculo de los dias se da hecho
		 */
		long diasDiff = DIAS_DE_ALQUILER;
		
		//Emplearé esta variable para tener siempre la fecha final correcta, tanto como si es nula como si no
		Date fechaFinAux = null;
		
		if (fechaFin != null) {
			
			fechaFinAux = fechaFin;
			diasDiff = TimeUnit.MILLISECONDS.toDays(fechaFin.getTime() - fechaIni.getTime());
			
			if (diasDiff < 1) {
				throw new AlquilerCochesException(AlquilerCochesException.SIN_DIAS);
			}
		}
		
		//Dejo la fecha de fin calculada en caso de que sea null la fechaFin introducida
		else {
			
			//Si la fecha de fin es null la calculo sumando a la de inicio los días de alquiler
			fechaFinAux = new Date(fechaIni.getTime() + TimeUnit.DAYS.toMillis(DIAS_DE_ALQUILER));
		}

		try {

			/* A completar por el alumnado... */

			/* ================================= AYUDA R�PIDA ===========================*/
			/*
			 * Algunas de las columnas utilizan tipo numeric en SQL, lo que se traduce en
			 * BigDecimal para Java.
			 * 
			 * Convertir un entero en BigDecimal: new BigDecimal(diasDiff)
			 * 
			 * Sumar 2 BigDecimals: usar metodo "add" de la clase BigDecimal
			 * 
			 * Multiplicar 2 BigDecimals: usar metodo "multiply" de la clase BigDecimal
			 *
			 * 
			 * Paso de util.Date a sql.Date: java.sql.Date sqlFechaIni = new
			 * java.sql.Date(sqlFechaIni.getTime());
			 *
			 *
			 * Recuerda que hay casos donde la fecha fin es nula, por lo que se debe de
			 * calcular sumando los dias de alquiler (ver variable DIAS_DE_ALQUILER) a la
			 * fecha ini.
			 * 
			 */
			
			//Inicializo la conexión a la base de datos
			con = pool.getConnection();
			
			/*---------------------------------------------------------------------------------------------------
			 * Para la gestion de las excepciones de cliente y vehículo inexistente plantearé un enfoque ofensivo.
			 * Primero gestionaré la excepción de vehiculo ocupado de manera defensiva, ya que de otra manera no 
			 * se detectará sola.
			 * Sin embargo, no detectaré si existe o no el cliente o el coche. Es decir, realizaré la insercion y si
			 * dichos elementos no existen, por la forma en la que están diseñadas las tablas, saltará una excepción 
			 * de clave foránea (2291)
			 * Por lo tanto será al capturarla cuando detecte que clave foránea ha sido violada y propague la excepción
			 * correcta.
			*-----------------------------------------------------------------------------------------*/
			
			//Ahora comprobaré la disponibilidad del coche introducido en las fechas especificadas
			selDisponible = con.prepareStatement("SELECT fecha_ini, fecha_fin FROM reservas WHERE matricula = ?");
			selDisponible.setString(1,matricula);
			
			cursor = selDisponible.executeQuery();
			
			//Defino unas variables de tipo Date donde iré guardando los valores de las fechas ya reservadas que nos devuelva la select
			Date fechaIniReservado = null;
			Date fechaFinReservado = null;
			
			//Itero por todas las reservas realizadas para dicho vehículo
			while(cursor.next()) {
				fechaIniReservado = cursor.getDate("fecha_ini");
				fechaFinReservado = cursor.getDate("fecha_fin");
				if (fechaFinReservado == null) {
					fechaFinReservado = new Date(fechaIniReservado.getTime() + TimeUnit.DAYS.toMillis(DIAS_DE_ALQUILER));
				}
				
				// Dos intervalos [A1,A2] y [B1,B2] se solapan si A1 <= B2 y B1 <= A2 por lo que haremos dicha comprobación
				// Emplearé: date1.after(date2) → true si date1 > date2
				if (!fechaIni.after(fechaFinReservado) && !fechaIniReservado.after(fechaFinAux)) {
					throw new AlquilerCochesException(AlquilerCochesException.VEHICULO_OCUPADO);
				}
			}
			cursor.close();
			
			/* 
			 * Una vez gestionada la principal excepción (vehículo ocupado) ya que las demás son violaciones de claves 
			 * foráneas, realizo la inserción de la reserva. Si no existe coche ya habrá saltado y si no existe cliente 
			 * saltará al intentar insertar.
			 */
			
			insReserva = con.prepareStatement(
					"INSERT INTO reservas " +
					"(idReserva, cliente, matricula, fecha_ini, fecha_fin) " + 
					"VALUES (seq_reservas.nextval,?,?,?,?)");
			
			insReserva.setString(1, nifCliente);
			insReserva.setString(2, matricula);
			
			//Para insertarlo en la tabla tengo que convertir las fechas de util.Date a sql.Date
			insReserva.setDate(3, new java.sql.Date(fechaIni.getTime()));
			
			//Como fechaFin puede ser null, empleo un operador ternario para gestionar dicha posibilidad
			//Si fechaFin es nula, la fecha en sql tendrá ese mismo valor, sino se realiza la conversión
			java.sql.Date sqlfechaFin = (fechaFin == null ? null : new java.sql.Date(fechaFin.getTime()));
			insReserva.setDate(4, sqlfechaFin);
			
			//Por último realizo la inserción en la tabla reservas
			insReserva.executeUpdate();
			
			//Ahora realizaré la inserción en la tabla facturas.
			insFactura = con.prepareStatement("INSERT INTO facturas (nroFactura, importe, cliente) VALUES ");
			
			
			//Si se ha llegado hasta aquí sin excepciones hacemos commit en la transacción
			con.commit();

		} catch (SQLException e) {
			
			//Cualquier excepción primero hacer rollback
			if (con != null) {
				con.rollback();
			}
			
			//Si es del mi tipo la propago
			if(e instanceof AlquilerCochesException) {
				throw (AlquilerCochesException) e;
			}
			
			if(new OracleSGBDErrorUtil().checkExceptionToCode(e, SGBDError.FK_VIOLATED)) {
				/*
				 * Como he explicado anteriormente al realizar la inserción saltará una excepción de violación de 
				 * clave foránea si no existe el cliente o el vehículo. Al capturar aquí dicha excepción detectaré
				 * que clave foránea ha sido violada
				 * Para ello emplearé bloques try-with-resources. Dichos bloques no necesitan finally ya que cierran
				 * los recursos automaticamente despúes de terminar de usarlos.
				 * Como estamos realizando una select dentro del bloque de excepciones necesitamos los bloques try
				 * ya que pueden saltar excepciones que no son de nuestro tipo.
				 */
				boolean existeCliente;
				try (PreparedStatement selFKFail = con.prepareStatement("SELECT NIF FROM clientes WHERE NIF = ?")){
					selFKFail.setString(1,nifCliente);
					try (ResultSet getFKFail = selFKFail.executeQuery();){
						existeCliente = getFKFail.next();
					}
				}
				
				// Si se ha entrado en este bloque significa que o el cliente o el coche no existe.
				if (!existeCliente) {
					throw new AlquilerCochesException(AlquilerCochesException.CLIENTE_NO_EXIST);
				}
				else {
					throw new AlquilerCochesException(AlquilerCochesException.VEHICULO_NO_EXIST);
				}
			}

			// Esto es lo q estaba por defecto en el archivo
			LOGGER.debug(e.getMessage());
			throw e;

		} finally {
			
			if (cursor != null) {
				cursor.close();
			}
			
			if (insReserva != null) {
				insReserva.close();
			}
			
			
			if (con != null) {
				con.close();
			}
		}
	}
}
