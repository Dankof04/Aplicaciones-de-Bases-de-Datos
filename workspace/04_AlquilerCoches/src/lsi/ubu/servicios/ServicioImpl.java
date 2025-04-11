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

public class ServicioImpl implements Servicio {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServicioImpl.class);

	private static final int DIAS_DE_ALQUILER = 4;

	public void alquilar(String nifCliente, String matricula, Date fechaIni, Date fechaFin) throws SQLException {
		PoolDeConexiones pool = PoolDeConexiones.getInstance();

		Connection con = null;
		PreparedStatement insReserva = null;
		PreparedStatement selReserva = null;
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
			 */
			
			//Inicializo la conexión a la base de datos
			con = pool.getConnection();
			
			// Defino strings con las consultas para hacer el proceso mas ordenado
			String sqlCliente = "SELECT NIF FROM clientes WHERE NIF = ?";
			String sqlCoche = "SELECT matricula FROM vehiculos WHERE matricula = ?";
			
			//Realizo la primera comprobacion para saber si existe el cliente
			selReserva = con.prepareStatement(sqlCliente);
			selReserva.setString(1,nifCliente);
			
			cursor = selReserva.executeQuery();
			if (!cursor.next()) {
				throw new AlquilerCochesException(AlquilerCochesException.CLIENTE_NO_EXIST);
			}
			
			//La existencia de coche la puedo comprobar igual o luego capturando FK_VIOLATED
			
			
			
			
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

			// Esto es lo q estaba por defecto en el archivo
			LOGGER.debug(e.getMessage());

			throw e;

		} finally {
			
			if (insReserva != null) {
				insReserva.close();
			}
			
			if (selReserva != null) {
				selReserva.close();
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
