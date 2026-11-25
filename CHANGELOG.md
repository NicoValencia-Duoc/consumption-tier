# Historial de cambios — THE MAW

## [0.9.8.3.1] — 2026-11-24

### Añadido
- Niveles de THE MAW por ítems terminados alguna vez, de SIN NIVEL a DEIDAD (100), con la barra de HAMBRE en el color de exceso al llegar a DEIDAD.
- Racha de días que cuenta las repeticiones (lista de fechas de término por ítem) y color de la racha en la marca.
- Pantalla de arranque de terminal con datos reales (registros, rechazados, nivel, hambre, racha) que usa el tema activo.
- Interruptores de efectos en OPCIONES: general, filtro CRT, escaneo, glitch y pantalla de arranque.
- Reformateo a coma de los decimales escritos con punto.
- Registros rechazados: se pueden revisar, corregir y recuperar, o descartar (con copia en `consumption-personal/descartados`).
- Botón COPIAR FICHA en el detalle (antes COPIAR DETALLES).

### Mejorado
- Pie con las cuatro zonas siempre visibles: la disposición se elige midiendo el contenido.
- El primer ítem de un nivel ya muestra avance en HAMBRE.
- Quitar la fecha de término de un ítem terminado ya no deja la fecha anterior en la racha.
- Escaneo: la altura sigue a la ventana; el glitch usa colores del tema.

### Mantenimiento
- Tildes y restos de depuración eliminados de textos y consola.
- Versiones alineadas entre `pom.xml`, `empaquetar.bat` y README.

## [0.9.3] — 2026-11-17

### Añadido
- Seis identidades visuales: SOLAR, ECLIPSE, ARCANO, ABISAL, ACERO y VECTOR.
- Compatibilidad de nombres de temas con preferencias anteriores.

### Mejorado
- Paletas y estilos de los seis temas.
- Apariencia de tablas, controles y tarjetas.
- Fondo negro de las áreas de imágenes de tarjetas.
- Estabilidad dimensional de la navegación superior.
- Tamaño de los diálogos de confirmación.
- Adaptación de ventanas y formularios.
- Visibilidad del efecto CRT en temas claros y oscuros.
- Alineación centrada de encabezados y datos en las tablas.

### Mantenimiento
- Eliminación conservadora de declaraciones CSS duplicadas.

### Compatibilidad
- Se mantienen los archivos internos de los temas y la lectura de preferencias anteriores.