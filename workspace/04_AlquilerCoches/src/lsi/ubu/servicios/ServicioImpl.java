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
		PreparedStatement selCliente = null;
		PreparedStatement selVehiculo = null;
		PreparedStatement selDisponible = null;
		PreparedStatement insReserva = null;
		ResultSet cursor = null;

		/*
		 * El calculo de los dias se da hecho
		 */
		long diasDiff = DIAS_DE_ALQUILER;
		if (fechaFin != null) {
			diasDiff = TimeUnit.MILLISECONDS.toDays(fechaFin.getTime() - fechaIni.getTime());

			if (diasDiff < 1) {
				throw new AlquilerCochesException(AlquilerCochesException.SIN_DIAS);
			}
		}
		
		//CREO Q NO SE PUEDE O NO SE HACE ASÍ: CORREGIR
		else {
			//Si la fecha de fin es null la calculo sumando a la de inicio los días de alquiler
			fechaFin = new Date(fechaIni.getTime() + TimeUnit.DAYS.toMillis(DIAS_DE_ALQUILER));
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
			 * Paso de util.Date a sql.Date java.sql.Date sqlFechaIni = new
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
			
			/*-----------------------------------------------------------------------------------------*/
			
			//Realizo la comprobacion para saber si existe el vehículo
			selVehiculo = con.prepareStatement("SELECT matricula FROM vehiculos WHERE matricula = ?");
			selVehiculo.setString(1,matricula);
			
			cursor = selVehiculo.executeQuery();
			if (!cursor.next()) {
				throw new AlquilerCochesException(AlquilerCochesException.VEHICULO_NO_EXIST);
			}
			cursor.close();
			
			//Si cuando insertemos el cliente este no existe, se violará la fk, se capturará en el catch y se propaga la correcta
			
			/*-----------------------------------------------------------------------------------------*/
			
			//Ahora comprobaré la disponibilidad del coche introducido en las fechas especificadas
			selDisponible = con.prepareStatement("SELECT fecha_ini, fecha_fin FROM reservas WHERE matricula = ?");
			selDisponible.setString(1,matricula);
			
			cursor = selDisponible.executeQuery();
			while(cursor.next()) {
				
			}
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			insReserva = con.prepareStatement("");
			
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
				//En caso de que se viole una clave foránea, compruebo cual es y saco la excepción adecuada
				//Pero si no se viola la clave no necesito comprobar nada y me lo ahorro
				//Sin embargo si compruebo fuera la de clientes, la uníca Fk q podríamos violar es la de coches
				throw new AlquilerCochesException(AlquilerCochesException.CLIENTE_NO_EXIST);
			}

			// Esto es lo q estaba por defecto en el archivo
			LOGGER.debug(e.getMessage());

			throw e;

		} finally {
			
			if (insReserva != null) {
				insReserva.close();
			}
			
			if (selCliente != null) {
				selCliente.close();
			}
			
			if (selVehiculo != null) {
				selVehiculo.close();
			}
			
			if (cursor != null) {
				cursor.close();
			}
			
			if (con != null) {
				con.close();
			}
		}
	}
}
