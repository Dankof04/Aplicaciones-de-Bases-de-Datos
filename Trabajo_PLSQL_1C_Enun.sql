DROP TABLE detalle_pedido CASCADE CONSTRAINTS;
DROP TABLE pedidos CASCADE CONSTRAINTS;
DROP TABLE platos CASCADE CONSTRAINTS;
DROP TABLE personal_servicio CASCADE CONSTRAINTS;
DROP TABLE clientes CASCADE CONSTRAINTS;

DROP SEQUENCE seq_pedidos;


-- Creación de tablas y secuencias



create sequence seq_pedidos;

CREATE TABLE clientes (
    id_cliente INTEGER PRIMARY KEY,
    nombre VARCHAR2(100) NOT NULL,
    apellido VARCHAR2(100) NOT NULL,
    telefono VARCHAR2(20)
);

CREATE TABLE personal_servicio (
    id_personal INTEGER PRIMARY KEY,
    nombre VARCHAR2(100) NOT NULL,
    apellido VARCHAR2(100) NOT NULL,
    pedidos_activos INTEGER DEFAULT 0 CHECK (pedidos_activos <= 5)
);

CREATE TABLE platos (
    id_plato INTEGER PRIMARY KEY,
    nombre VARCHAR2(100) NOT NULL,
    precio DECIMAL(10, 2) NOT NULL,
    disponible INTEGER DEFAULT 1 CHECK (DISPONIBLE in (0,1))
);

CREATE TABLE pedidos (
    id_pedido INTEGER PRIMARY KEY,
    id_cliente INTEGER REFERENCES clientes(id_cliente),
    id_personal INTEGER REFERENCES personal_servicio(id_personal),
    fecha_pedido DATE DEFAULT SYSDATE,
    total DECIMAL(10, 2) DEFAULT 0
);

CREATE TABLE detalle_pedido (
    id_pedido INTEGER REFERENCES pedidos(id_pedido),
    id_plato INTEGER REFERENCES platos(id_plato),
    cantidad INTEGER NOT NULL,
    PRIMARY KEY (id_pedido, id_plato)
);


	
-- Procedimiento a implementar para realizar la reserva
create or replace procedure registrar_pedido(
    arg_id_cliente      INTEGER, 
    arg_id_personal     INTEGER, 
    arg_id_primer_plato INTEGER DEFAULT NULL,
    arg_id_segundo_plato INTEGER DEFAULT NULL
) is 

    -- DECLARACIÓN DE EXCEPCIONES
    plato_no_disponible EXCEPTION;
    PRAGMA EXCEPTION_INIT(plato_no_disponible, -20001);
    pedido_sin_plato EXCEPTION;
    PRAGMA EXCEPTION_INIT(pedido_sin_plato, -20002);
    personal_sin_hueco_disponible EXCEPTION;
    PRAGMA EXCEPTION_INIT(personal_sin_hueco_disponible, -20003);
    plato_inexistente EXCEPTION;
    PRAGMA EXCEPTION_INIT(plato_inexistente, -20004);
    
    --DECLARACIÓN DEL CURSOR
    CURSOR c_plato (v_id_plato INTEGER) IS
        SELECT precio,disponible
        FROM platos
        WHERE id_plato = v_id_plato;
    
    --DECLARACIÓN DE VARIABLES
    v_precioPlato DECIMAL(10, 2);
    v_disponibilidad INTEGER;
    v_precioTotal DECIMAL(10, 2);
    v_cantidadPlato INTEGER;
    v_numPedidos INTEGER;
    

 begin
    --Inicializo la variable del precio total del pedido y la cantidad de cada plato
    v_precioTotal:=0;
    v_cantidadPlato:=1;
    
    --Comprobación de que el pedido contiene algún plato
    IF arg_id_primer_plato IS NULL AND arg_id_segundo_plato IS NULL
    THEN
        raise_application_error(-20002, 'El pedido debe contener al menos un plato');
    END IF;
    
    --Realizo las comprobaciones para el primer plato
    IF arg_id_primer_plato IS NOT NULL
    THEN
        OPEN c_plato(arg_id_primer_plato);
        FETCH c_plato INTO v_precioPlato,v_disponibilidad;
        IF c_plato%NOTFOUND THEN
            raise_application_error(-20004, 'El primer plato seleccionado no existe');
        ELSIF v_disponibilidad = 0 THEN
            raise_application_error(-20001, 'Uno de los platos seleccionados no esta disponible');
        END IF;
        v_precioTotal:=v_precioTotal+v_precioPlato;
        CLOSE c_plato;
    END IF;
    
    --Si el primer plato es correcto y los dos ids son iguales(mismo plato)
    --Modifico solo dos variables y me ahorro mas comprobación de excepciones
    IF arg_id_primer_plato = arg_id_segundo_plato THEN
        v_precioTotal:=v_precioTotal+v_precioPlato;
        v_cantidadPlato:=2;
        
    --Sino realizo las comprobaciones para el segundo plato
    ELSIF arg_id_segundo_plato IS NOT NULL
    THEN
        OPEN c_plato(arg_id_segundo_plato);
        FETCH c_plato INTO v_precioPlato,v_disponibilidad;
        IF c_plato%NOTFOUND THEN
            raise_application_error(-20004, 'El segundo plato seleccionado no existe');
        ELSIF v_disponibilidad = 0 THEN
            raise_application_error(-20001, 'Uno de los platos seleccionados no esta disponible');
        END IF;
        v_precioTotal:=v_precioTotal+v_precioPlato;
        CLOSE c_plato;
    END IF;
        
        
    --Si hemos llegado aquí es que los platos existen y están disponibles.    
    --En esta parte actualizo los pedidos del personal, lo bloqueo para escritura
    --Gracias al bloqueo evito que otra transacción modifique el dato hasta que yo finalice
    SELECT pedidos_activos INTO v_numPedidos FROM personal_servicio
    WHERE personal_servicio.id_personal=arg_id_personal FOR UPDATE;
    
    --Si en esta parte viola la constraint saltara excepcion y la capturo en su bloque
    --Esta es la ultima excepción que podría saltar en el proceso
    UPDATE personal_servicio
    SET pedidos_activos = v_numPedidos + 1
    WHERE personal_servicio.id_personal=arg_id_personal;
    
    --Inserto el pedido en la tabla de pedidos
    INSERT INTO pedidos (id_pedido, id_cliente, id_personal,total)
    VALUES (seq_pedidos.nextval, arg_id_cliente, arg_id_personal, v_precioTotal);
    
    --Inserto los detallles del pedido en la tabla correspondiente
    IF v_cantidadPlato=2 THEN
        INSERT INTO detalle_pedido (id_pedido, id_plato, cantidad)
        VALUES (seq_pedidos.currval,arg_id_primer_plato,2);
    ELSE
        IF arg_id_primer_plato IS NOT NULL
        THEN
            INSERT INTO detalle_pedido (id_pedido, id_plato, cantidad)
            VALUES (seq_pedidos.currval,arg_id_primer_plato,1);
        END IF;
        
        IF arg_id_segundo_plato IS NOT NULL
        THEN
            INSERT INTO detalle_pedido (id_pedido, id_plato, cantidad)
            VALUES (seq_pedidos.currval,arg_id_segundo_plato,1);
        END IF;
    END IF;
    
    commit;
    
 exception   
    when others then
        IF SQLCODE=-2290 THEN
            rollback;
            raise_application_error(-20003, 'El personal de servicio tiene demasiados pedidos');
        ELSE
            rollback;
            raise;
        END IF;
            
end;
/

------ Deja aquí tus respuestas a las preguntas del enunciado:
-- NO SE CORREGIRÁN RESPUESTAS QUE NO ESTÉN AQUÍ (utiliza el espacio que necesites apra cada una)
-- * P4.1
--
-- * P4.2
--
-- * P4.3
--
-- * P4.4
--
-- * P4.5
-- 


create or replace
procedure reset_seq( p_seq_name varchar )
is
    l_val number;
begin
    execute immediate
    'select ' || p_seq_name || '.nextval from dual' INTO l_val;

    execute immediate
    'alter sequence ' || p_seq_name || ' increment by -' || l_val || 
                                                          ' minvalue 0';
    execute immediate
    'select ' || p_seq_name || '.nextval from dual' INTO l_val;

    execute immediate
    'alter sequence ' || p_seq_name || ' increment by 1 minvalue 0';

end;
/


create or replace procedure inicializa_test is
begin
    
    reset_seq('seq_pedidos');
        
  
    delete from Detalle_pedido;
    delete from Pedidos;
    delete from Platos;
    delete from Personal_servicio;
    delete from Clientes;
    
    -- Insertar datos de prueba
    insert into Clientes (id_cliente, nombre, apellido, telefono) values (1, 'Pepe', 'Perez', '123456789');
    insert into Clientes (id_cliente, nombre, apellido, telefono) values (2, 'Ana', 'Garcia', '987654321');
    
    insert into Personal_servicio (id_personal, nombre, apellido, pedidos_activos) values (1, 'Carlos', 'Lopez', 0);
    insert into Personal_servicio (id_personal, nombre, apellido, pedidos_activos) values (2, 'Maria', 'Fernandez', 5);
    
    insert into Platos (id_plato, nombre, precio, disponible) values (1, 'Sopa', 10.0, 1);
    insert into Platos (id_plato, nombre, precio, disponible) values (2, 'Pasta', 12.0, 1);
    insert into Platos (id_plato, nombre, precio, disponible) values (3, 'Carne', 15.0, 0);

    commit;
end;
/

exec inicializa_test;

-- Completa lost test, incluyendo al menos los del enunciado y añadiendo los que consideres necesarios

create or replace procedure test_registrar_pedido is
begin
	 
  --caso 1 Pedido correct, se realiza
  begin
    inicializa_test;
  end;
  
  -- Idem para el resto de casos

  /* - Si se realiza un pedido vac´ıo (sin platos) devuelve el error -200002.
     - Si se realiza un pedido con un plato que no existe devuelve en error -20004.
     - Si se realiza un pedido que incluye un plato que no est´a ya disponible devuelve el error -20001.
     - Personal de servicio ya tiene 5 pedidos activos y se le asigna otro pedido devuelve el error -20003
     - ... los que os puedan ocurrir que puedan ser necesarios para comprobar el correcto funcionamiento del procedimiento
*/
  
end;
/


set serveroutput on;
exec test_registrar_pedido;