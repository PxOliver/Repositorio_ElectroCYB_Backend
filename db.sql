-- 1. CREACIÓN DE LA BASE DE DATOS
----------------------------------
--CREATE DATABASE IF NOT EXISTS electrocyb_db;

--USE electrocyb_db;

-- 2. CREACIÓN DE LA TABLA 'users'
----------------------------------
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL
);

-- 3. CREACIÓN DE LA TABLA 'productos'
-------------------------------------
CREATE TABLE IF NOT EXISTS productos (
    id INTEGER PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    precio VARCHAR(50) NOT NULL,
    imagen VARCHAR(255),
    descripcion TEXT,
    categoria VARCHAR(100),
    stock INTEGER DEFAULT 0
);

-- 4. CREACIÓN DE LA TABLA 'producto_caracteristicas'
-----------------------------------------------------
CREATE TABLE IF NOT EXISTS producto_caracteristicas (
    id SERIAL PRIMARY KEY,
    producto_id INTEGER NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    valor VARCHAR(255),
    FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE
);

-- 5. INSERCIÓN DE DATOS
-------------------------

-- 5.1. Datos de la tabla 'users'
INSERT INTO users (email, full_name, password, phone, role)
VALUES (
    'admin@admin.com',
    'Administrador',
    '$2a$10$rqE.z3196kzXOIlgnC4ao.ALRrwQhV3/eyiXz3H8mxTAzdDvrpLiu',
    '999999999',
    'ADMIN'
);

-- 5.2. Datos de la tabla 'productos'

INSERT INTO productos (id, nombre, precio, imagen, descripcion, categoria, stock) VALUES
(13, 'Manguera LED Navideña', '3.50 x metro', '/uploads/productos/IMG-20250806-WA0019.jpg', 'Manguera LED Navideña, luz continua, flexible ideal para Proyectos y Figuras navideñas.', 'Tiras LED', 72);

INSERT INTO productos (id, nombre, precio, imagen, descripcion, categoria, stock) VALUES
(14, 'Kit Solar con Radio y Linterna', '50.00', '/uploads/productos/IMG-20250806-WA0038.jpg', 'Kit Solar con Radio y Linterna, ideal para emergencias y actividades al aire libre.', 'Linternas', 48);

INSERT INTO productos (id, nombre, precio, imagen, descripcion, categoria, stock) VALUES
(15, 'Estaño para Soldar', '80.00', '/uploads/productos/IMG-20250806-WA0041.jpg', 'Estaño para soldar 1mm x 500g, ideal para proyectos electrónicos y eléctricos.', 'Accesorios', 20);

INSERT INTO productos (id, nombre, precio, imagen, descripcion, categoria, stock) VALUES
(16, 'Modulos LED ', '2.50 x Unidad', '/uploads/productos/IMG-20250806-WA0042.jpg', 'Modulos LED ideal para letreros y Proyectos.', 'Tiras LED', 100);

INSERT INTO productos (id, nombre, precio, imagen, descripcion, categoria, stock) VALUES
(17, 'Luminaria LED', 'Consultar', '/uploads/productos/IMG-20250806-WA0043.jpg', 'Luminaria LED ideal para Espacios grandes.', 'Lámparas', 5);

INSERT INTO productos (id, nombre, precio, imagen, descripcion, categoria, stock) VALUES
(18, 'Extension de 16 puertos ', '50.00', '/uploads/productos/IMG-20250806-WA0060.jpg', 'Extension de 16 puertos con interruptor individual y proteccion contra sobrecargas.', 'Accesorios', 85);

INSERT INTO productos (id, nombre, precio, imagen, descripcion, categoria, stock) VALUES
(19, 'Foco LED 45W Mariposa', '30.00', '/uploads/productos/IMG-20250806-WA0047.jpg', 'Foco de luz Blanca.', 'Focos LED', 67);

INSERT INTO productos (id, nombre, precio, imagen, descripcion, categoria, stock) VALUES
(20, 'Foco LED Recargable con Panel Solar', '30.00', '/uploads/productos/IMG-20250806-WA0065.jpg', 'Foco de luz Blanca Recargable con Panel Solar.', 'Focos LED Recargables', 18);

INSERT INTO productos (id, nombre, precio, imagen, descripcion, categoria, stock) VALUES
(21, 'Foco LED Recargable', '20.00', '/uploads/productos/IMG-20250806-WA0066.jpg', 'Foco de luz Blanca Recargable.', 'Focos LED Recargables', 44);

INSERT INTO productos (id, nombre, precio, imagen, descripcion, categoria, stock) VALUES
(22, 'Foco LED 36W Mariposa', '20.00', '/uploads/productos/IMG-20250806-WA0049.jpg', 'Foco de luz Blanca con Hojas para un mejor alcance de luz.', 'Focos LED', 96);

INSERT INTO productos (id, nombre, precio, imagen, descripcion, categoria, stock) VALUES
(23, 'Reflectores LED', 'Consultar', '/uploads/productos/IMG-20250806-WA0053.jpg', 'Reflectores LED de alta potencia para exteriores. Ideal para interiores y fachadas.', 'Reflectores LED', 31);

INSERT INTO productos (id, nombre, precio, imagen, descripcion, categoria, stock) VALUES
(24, 'Foco LED 3W', '5.00', '/uploads/productos/IMG-20250806-WA0067.jpg', 'Foco de luz BLanca.', 'Focos LED', 79);

INSERT INTO productos (id, nombre, precio, imagen, descripcion, categoria, stock) VALUES
(25, 'Foco LED RGB', '10.00', '/uploads/productos/IMG-20250806-WA0070.jpg', 'Foco de luz RGB ideal para ambientes.', 'Focos LED RGB', 60);

INSERT INTO productos (id, nombre, precio, imagen, descripcion, categoria, stock) VALUES
(26, 'Foco LED Dimmer', '15.00', '/uploads/productos/IMG-20250806-WA0073.jpg', 'Foco de luz BLanca variable con 3 niveles de iluminacion.', 'Focos LED', 27);

INSERT INTO productos (id, nombre, precio, imagen, descripcion, categoria, stock) VALUES
(27, 'Foco LED 9w Calida', '7.00', '/uploads/productos/IMG-20250806-WA0074.jpg', 'Foco de luz calida.', 'Focos LED', 50);

INSERT INTO productos (id, nombre, precio, imagen, descripcion, categoria, stock) VALUES
(28, 'Lampara de Emergencia LED Recargable', '50.00', '/uploads/productos/IMG-20250806-WA0054.jpg', 'Lampara de Emergencia LED Recargable', 'Lamparas de Emergencia', 83);

INSERT INTO productos (id, nombre, precio, imagen, descripcion, categoria, stock) VALUES
(29, 'Lampara de Emergencia LED Recargable', '60.00', '/uploads/productos/IMG-20250806-WA0055.jpg', 'Lampara de Emergencia LED Recargable con forma de ojos', 'Lamparas de Emergencia', 36);
-- 5.3. Datos de la tabla 'producto_caracteristicas'
-- Producto 1
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(1, 'potencia', '24W'),
(1, 'voltaje', '220V'),
(1, 'temperatura_color', '10000K (Luz Blanca)'),
(1, 'flujo_luminoso', '5000 lúmenes'),
(1, 'vida_util', '50,000 horas'),
(1, 'material', 'Aluminio y acrílico'),
(1, 'dimensiones', '40cm de diámetro x 5cm de altura'),
(1, 'instalacion', 'Base E27 Estandar'),
(1, 'garantia', '2 años');

-- Producto 2
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(2, 'potencia', '10A'),
(2, 'voltaje', '220V a 12V'),
(2, 'material', 'Policarbonato y aluminio'),
(2, 'dimensiones', '20cm x 10cm x 5cm'),
(2, 'instalacion', 'Depende del proyecto'),
(2, 'control', 'Manual'),
(2, 'garantia', '1 año');

-- Producto 3
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(3, 'potencia', '18W LED'),
(3, 'voltaje', '220V'),
(3, 'temperatura_color', '2700K (Luz Blanca)'),
(3, 'flujo_luminoso', '1620 lúmenes'),
(3, 'vida_util', '25,000 horas'),
(3, 'material', 'Metal blanco mate'),
(3, 'dimensiones', '25cm de diámetro x 30cm de altura'),
(3, 'instalacion', 'Empotrable en techo falso'),
(3, 'estilo', 'Industrial Moderno'),
(3, 'garantia', '1 año');

-- Producto 4
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(4, 'Luces', 'Luz Ultravioleta y Blanca'),
(4, 'voltaje', '220V'),
(4, 'material', 'Plastico'),
(4, 'dimensiones', '10cm x 10cm x 10cm'),
(4, 'instalacion', 'Depende del proyecto'),
(4, 'garantia', '1 año');

-- Producto 5
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(5, 'potencia', '9W'),
(5, 'voltaje', '220V'),
(5, 'colores', 'RGB'),
(5, 'vida_util', '30,000 horas'),
(5, 'material', 'Plastico'),
(5, 'dimensiones', '10cm de altura x 10cm de base'),
(5, 'instalacion', 'Depende del proyecto'),
(5, 'control', 'Base E27 Estandar'),
(5, 'garantia', '1 año');

-- Producto 6
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(6, 'potencia', '5W'),
(6, 'voltaje', '220V'),
(6, 'temperatura_color', '3000K (Luz cálida)'),
(6, 'flujo_luminoso', '700 lúmenes'),
(6, 'vida_util', '35,000 horas'),
(6, 'material', 'Vidrio'),
(6, 'dimensiones', '8cm de diámetro'),
(6, 'instalacion', 'Base E27 Estandar'),
(6, 'garantia', '1 año');

-- Producto 7
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(7, 'potencia', '72W (5 metros)'),
(7, 'voltaje', '12V DC'),
(7, 'longitud', '5 metros'),
(7, 'leds_por_metro', '60 LEDs/metro'),
(7, 'vida_util', '50,000 horas'),
(7, 'material', 'Silicona flexible'),
(7, 'colores', '16 de colores RGB'),
(7, 'control', 'Controlador Manual Incluido'),
(7, 'incluye', 'Fuente de poder y controlador'),
(7, 'garantia', '1 año');

-- Producto 8
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(8, 'potencia', '15W'),
(8, 'voltaje', '220V'),
(8, 'temperatura_color', '6500K (Luz fría)'),
(8, 'flujo_luminoso', '10,000 lúmenes'),
(8, 'vida_util', '50,000 horas'),
(8, 'material', 'Aluminio y acrílico'),
(8, 'dimensiones', '35cm x 28cm x 5cm'),
(8, 'instalacion', 'Empotrable en techo falso'),
(8, 'uso', 'Uso interiores'),
(8, 'garantia', '1 año');

-- Producto 9
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(9, 'temperatura_color', '6500K (Luz fría)'),
(9, 'vida_util', '50,000 horas'),
(9, 'material', 'Plastico'),
(9, 'incluye', 'Panel solar, Radio, Bluetooth, Reflector, Cargador de Telefonos y 3 focos de 3w'),
(9, 'dimensiones', '35cm x 28cm x 5cm'),
(9, 'instalacion', 'Depende del proyecto'),
(9, 'uso', 'Uso de interiores y exteriores'),
(9, 'garantia', '1 año');

-- Producto 10
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(10, 'colores', 'Rojo, Azul, Verde, Blanco, RGB'),
(10, 'vida_util', '50,000 horas'),
(10, 'material', 'Plastico'),
(10, 'dimensiones', '5 metros'),
(10, 'instalacion', 'Depende del proyecto'),
(10, 'uso', 'Uso de interiores y exteriores'),
(10, 'garantia', '1 año');

-- Producto 11
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(11, 'color', 'RGB'),
(11, 'vida_util', '50,000 horas'),
(11, 'material', 'Plastico'),
(11, 'dimensiones', '100 metros x Rollo'),
(11, 'instalacion', 'Depende del proyecto'),
(11, 'uso', 'Uso de interiores y exteriores'),
(11, 'garantia', '1 año');

-- Producto 12
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(12, 'color', 'Blanca y Cálida, los demas colores a pedido'),
(12, 'vida_util', '50,000 horas'),
(12, 'material', 'Plastico'),
(12, 'dimensiones', '100 metros x Rollo'),
(12, 'instalacion', 'Depende del proyecto'),
(12, 'uso', 'Uso de interiores y exteriores'),
(12, 'garantia', '1 año');

-- Producto 13
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(13, 'color', 'Blanca, Azul, Roja, Verde, Calida, Multicolor'),
(13, 'vida_util', '50,000 horas'),
(13, 'material', 'Plastico'),
(13, 'dimensiones', '100 metros x Rollo'),
(13, 'instalacion', 'Depende del proyecto'),
(13, 'uso', 'Uso de interiores y exteriores'),
(13, 'garantia', '1 año');

-- Producto 14
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(14, 'incluye', 'Panel solar, Radio, Bluetooth, Reflector, Cargador de Telefonos y 3 focos de 3w.'),
(14, 'temperatura_color', '6500K (Luz fría)'),
(14, 'vida_util', '50,000 horas'),
(14, 'material', 'Plastico'),
(14, 'dimensiones', '35cm x 28cm x 5cm'),
(14, 'instalacion', 'Depende del proyecto'),
(14, 'uso', 'Uso de interiores y exteriores'),
(14, 'garantia', '1 año');

-- Producto 15
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(15, 'dimensiones', '1mm x 500g'),
(15, 'instalacion', 'Depende del proyecto'),
(15, 'uso', 'Proyectos electrónicos y eléctricos'),
(15, 'garantia', 'No aplica');

-- Producto 16
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(16, 'voltaje', '12V'),
(16, 'color', 'Blanca'),
(16, 'vida_util', '50,000 horas'),
(16, 'material', 'Plastico'),
(16, 'dimensiones', '5cm x 2cm'),
(16, 'uso', 'Depende del proyecto'),
(16, 'garantia', 'No aplica');

-- Producto 17
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(17, 'voltaje', '220V'),
(17, 'color', 'Blanca, Cálida, Neutra'),
(17, 'vida_util', '50,000 horas'),
(17, 'material', 'Plastico'),
(17, 'dimensiones', 'Varía según el modelo'),
(17, 'instalacion', 'Empotrable en techo falso'),
(17, 'uso', 'Depende del proyecto'),
(17, 'garantia', 'No aplica');

-- Producto 18
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(18, 'dimensiones', '20cm x 10cm x 5cm'),
(18, 'instalacion', 'Enchufe a 220V'),
(18, 'uso', 'Depende del proyecto'),
(18, 'garantia', 'No aplica');

-- Producto 19
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(19, 'potencia', '45W'),
(19, 'voltaje', '220V'),
(19, 'temperatura_color', '6000K (Luz Blanca)'),
(19, 'flujo_luminoso', '700 lúmenes'),
(19, 'vida_util', '35,000 horas'),
(19, 'material', 'Plastico'),
(19, 'dimensiones', '20cm de diámetro'),
(19, 'instalacion', 'Base E27 Estandar'),
(19, 'garantia', '1 año');

-- Producto 20
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(20, 'potencia', '45W'),
(20, 'voltaje', '5v USB'),
(20, 'temperatura_color', '6000K (Luz Blanca)'),
(20, 'flujo_luminoso', '700 lúmenes'),
(20, 'vida_util', '35,000 horas'),
(20, 'material', 'Plastico'),
(20, 'dimensiones', '20cm de diámetro'),
(20, 'instalacion', 'Depende del proyecto'),
(20, 'garantia', '1 año');

-- Producto 21
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(21, 'potencia', '45W'),
(21, 'voltaje', '5v USB'),
(21, 'temperatura_color', '6000K (Luz Blanca)'),
(21, 'flujo_luminoso', '700 lúmenes'),
(21, 'vida_util', '35,000 horas'),
(21, 'material', 'Plastico'),
(21, 'dimensiones', '20cm de diámetro'),
(21, 'instalacion', 'Depende del proyecto'),
(21, 'garantia', '1 año');

-- Producto 22
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(22, 'potencia', '36W'),
(22, 'voltaje', '220V'),
(22, 'colores', 'Blanca y Cálida'),
(22, 'vida_util', '35,000 horas'),
(22, 'material', 'Plastico'),
(22, 'dimensiones', '20cm de diámetro'),
(22, 'instalacion', 'Base E27 Estandar'),
(22, 'garantia', '1 año');

-- Producto 23
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(23, 'potencia', 'Depende del modelo'),
(23, 'voltaje', '12V - 24V'),
(23, 'colores', 'Blanco'),
(23, 'vida_util', '35,000 horas'),
(23, 'material', 'Policarbonato y aluminio'),
(23, 'dimensiones', 'Depende del modelo'),
(23, 'instalacion', 'Depende del proyecto'),
(23, 'garantia', '1 año');

-- Producto 24
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(24, 'potencia', '3W'),
(24, 'voltaje', '220V'),
(24, 'temperatura_color', '7000K (Luz Blanca)'),
(24, 'flujo_luminoso', '220 lúmenes'),
(24, 'vida_util', '35,000 horas'),
(24, 'material', 'Vidrio'),
(24, 'dimensiones', '8cm de diámetro'),
(24, 'instalacion', 'Base E27 Estandar'),
(24, 'garantia', '1 año');

-- Producto 25
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(25, 'potencia', '10W'),
(25, 'voltaje', '220V'),
(25, 'colores', 'RGB'),
(25, 'vida_util', '35,000 horas'),
(25, 'material', 'Plastico'),
(25, 'dimensiones', '8cm de diámetro'),
(25, 'instalacion', 'Base E27 Estandar'),
(25, 'garantia', '1 año');

-- Producto 26
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(26, 'potencia', '9W'),
(26, 'voltaje', '220V'),
(26, 'temperatura_color', '7000K (Luz Blanca)'),
(26, 'flujo_luminoso', '800 lúmenes'),
(26, 'vida_util', '25,000 horas'),
(26, 'material', 'Plastico'),
(26, 'dimensiones', '8cm de diámetro'),
(26, 'instalacion', 'Base E27 Estandar'),
(26, 'garantia', '1 año');

-- Producto 27
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(27, 'potencia', '7W'),
(27, 'voltaje', '220V'),
(27, 'temperatura_color', '3000K (Luz cálida)'),
(27, 'flujo_luminoso', '450 lúmenes'),
(27, 'vida_util', '35,000 horas'),
(27, 'material', 'Vidrio'),
(27, 'dimensiones', '8cm de diámetro'),
(27, 'instalacion', 'Base E27 Estandar'),
(27, 'garantia', '1 año');

-- Producto 28
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(28, 'potencia', '30W'),
(28, 'voltaje', '220V'),
(28, 'temperatura_color', '6000K (Luz Blanca)'),
(28, 'vida_util', '35,000 horas'),
(28, 'material', 'Plastico'),
(28, 'dimensiones', '25cm de altura x 15cm de base'),
(28, 'instalacion', 'Enchufe a 220V'),
(28, 'garantia', '1 año');

-- Producto 29
INSERT INTO producto_caracteristicas (producto_id, nombre, valor) VALUES
(29, 'potencia', '30W'),
(29, 'voltaje', '220V'),
(29, 'temperatura_color', '6000K (Luz Blanca)'),
(29, 'vida_util', '35,000 horas'),
(29, 'material', 'Plastico'),
(29, 'dimensiones', '25cm de altura x 15cm de base'),
(29, 'instalacion', 'Enchufe a 220V'),
(29, 'garantia', '1 año');