# PreuSync

**Plataforma educativa integral para estudiantes preuniversitarios.**

[![Android](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white)](https://developer.android.com)
[![Java](https://img.shields.io/badge/Java-ED8B00?style=flat&logo=java&logoColor=white)](https://www.java.com/)
[![Supabase](https://img.shields.io/badge/Supabase-3ECF8E?style=flat&logo=supabase&logoColor=white)](https://supabase.com/)
[![Express](https://img.shields.io/badge/Express.js-404D59?style=flat&logo=express&logoColor=white)](https://expressjs.com/)

---

## Descripción

**PreuSync** es una aplicación móvil diseñada para la comunidad educativa de los preuniversitarios. Centraliza noticias, eventos, horarios, publicaciones interactivas y efemérides en un solo lugar, fomentando la comunicación y el acceso a la información entre estudiantes, tutores y docentes.

### Objetivo

Proporcionar una herramienta moderna, gratuita y sin publicidad que facilite la vida académica de miles de estudiantes preuniversitarios, mejorando su organización y acceso a recursos educativos.

---

## Características principales

- **Autenticación segura** – Registro e inicio de sesión con JWT y almacenamiento cifrado.
- **Feed de inicio** – Noticias destacadas, posts populares, efeméride del día y turno actual en tiempo real.
- **Noticias** – Fuentes de información confiables con enlaces a las fuentes originales.
- **Posts interactivos** – Publicaciones con imágenes, votos (like/dislike), comentarios y reportes.
- **Eventos** – Calendario de eventos escolares y externos con filtros y recordatorios.
- **Horario escolar** – Tabla dinámica con resaltado del turno actual.
- **Efemérides** – Consulta diaria de efemérides históricas con imágenes y detalles.
- **Perfil de usuario** – Edición de datos personales, avatar personalizado y estadísticas.
- **Notificaciones push** – Alertas personalizables para noticias, posts, eventos y cambios de horario.
- **Modo offline** – Datos cacheados localmente para consulta sin conexión.
- **Temas dinámicos** – Soporte para Material You (Android 12+), modo oscuro y claro.
- **Multi-idioma** – Español e Inglés (más idiomas planificados).
- **Accesibilidad** – Contraste mejorado, tamaños de fuente ajustables y etiquetas para lectores de pantalla.

---

## Tecnologías utilizadas

### Cliente (Android)
- **Lenguaje:** Java 17
- **Arquitectura:** MVVM con ViewModel y LiveData
- **UI:** Material Design 3, ViewPager2, RecyclerView, BottomNavigationView
- **Red:** OkHttp + RequestNetwork (wrapper propio)
- **Almacenamiento:** SharedPreferences (cifrado con Android Keystore) + caché de archivos (XOR)
- **Imágenes:** Glide
- **Markdown/LaTeX:** CommonMark + Prism4j + JLatexMath
- **Background:** WorkManager + AlarmManager
- **Notificaciones:** NotificationManagerCompat

### Backend (API)
- **Entorno:** Node.js (ES Modules)
- **Framework:** Express 5
- **Base de datos:** Supabase (PostgreSQL) con RLS
- **Autenticación:** JWT (Supabase Auth)
- **Almacenamiento de archivos:** Supabase Storage (bucket `user-files`)
- **Seguridad:** Helmet, CORS, compression, morgan
- **Despliegue:** Render.com (o cualquier plataforma Node.js)

---

## Estructura del proyecto

```

PreuSync/
├── android/                     # Código fuente de la app Android
│   ├── app/
│   │   ├── src/main/java/...    # Paquetes de la app
│   │   ├── res/                 # Layouts, drawables, valores
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── backend/                     # Servidor Node.js
│   ├── src/
│   │   ├── config/              # Configuración (Supabase)
│   │   ├── middleware/          # Autenticación
│   │   ├── routes/              # Endpoints
│   │   ├── services/            # Lógica de negocio
│   │   └── utils/               # Utilidades (storage)
│   ├── .env.example             # Variables de entorno
│   ├── package.json
│   └── server.js
├── README.md
└── LICENSE

```

---

## Instalación y configuración

### Cliente (Android)

1. **Clona el repositorio**
   ```bash
   git clone https://github.com/JiroxDEV/PreuSync.git
   cd PreuSync/android
   ```

2. **Abre el proyecto en Android Studio** (versión 2022.3 o superior).
3. **Configura las variables de entorno** (opcional):
   Si deseas apuntar a un backend local, modifica `PreferenceConstants.API_BASE_URL en utils/common/PreferenceConstants.java`.
4. **Compila y ejecuta** sobre un emulador o dispositivo físico.

### Backend (API)

1. **Navega al directorio del backend**
   ```bash
   cd PreuSync/backend
   ```
2. **Instala las dependencias**
   ```bash
   npm install
   ```
3. **Crea un archivo** `.env` **basado en** `.env.example` **y completa tus credenciales de Supabase:**
   ```env
   SUPABASE_URL=https://tu-proyecto.supabase.co
   SUPABASE_ANON_KEY=tu-clave-anon-publica
   SUPABASE_SERVICE_ROLE_KEY=tu-clave-service-role
   PORT=3000
   NODE_ENV=development
   ```
4. **Inicia el servidor**
   ```bash
   npm run dev   # desarrollo (con nodemon)
   npm start     # producción
   ```
5. **Verifica que la API responde**
   Accede a http://localhost:3000/health o http://localhost:3000/api/ping.

---

### Configuración de Supabase

1. **Crea un proyecto en Supabase.**
2. **Ejecuta las migraciones SQL** (proporcionadas en `backend/supabase/migrations/`) para crear las tablas:
    - `profiles`
    - `posts`
    - `news`
    - `events`
    - `schedules`
    - `ephemerides`
    - `votes`
    - `reports`
    - `app_versions`
3. **Configura Row Level Security** (RLS) según las políticas incluidas.
4. **Crea un bucket público llamado** `user-files` **para almacenar imágenes.**
5. **Añade la tabla** `app_versions` **y puebla con los changelogs proporcionados.**

---

## Licencia y Copyright

PreuSync™ es un proyecto de BinaryQva™. Desarrollado por JiroxDEV.

© 2026 Daniel Alejandro Pupo González (JiroxDEV). PreuSync™ es un proyecto de BinaryQva™.
Publicado bajo la Licencia **GNU Affero General Public License v3 (AGPLv3)**.

---

## Licencias de herramientas y servicios de terceros

PreuSync utiliza una serie de librerías, herramientas y servicios de terceros que facilitan su desarrollo y funcionamiento. A continuación se enumeran todas ellas, junto con sus respectivas licencias y avisos de derechos de autor, en cumplimiento con los términos de uso de cada una.

### Cliente Android (Java)

| Herramienta / Librería       | Propósito                                  | Licencia         | Enlace                                                             | Aviso de copyright                                        |
|------------------------------|--------------------------------------------|------------------|--------------------------------------------------------------------|-----------------------------------------------------------|
| Android SDK                  | Plataforma de desarrollo para Android      | AGPL-3.0       | developer.android.com                                              | Copyright © Google LLC. All rights reserved.              |
| AndroidX                     | Librerías de soporte para Android          | AGPL-3.0       | developer.android.com/jetpack/androidx                             | Copyright © Google LLC. All rights reserved.              |
| Material Design 3            | Componentes de interfaz de usuario         | AGPL-3.0       | github.com/material-components                                     | Copyright © Google LLC. All rights reserved.              |
| Glide                        | Carga y caché de imágenes                  | BSD 2-Clause     | github.com/bumptech/glide                                          | Copyright © 2014 Google, Inc. All rights reserved.        |
| OkHttp                       | Cliente HTTP para redes                    | AGPL-3.0       | github.com/square/okhttp                                           | Copyright © 2019 Square, Inc.                             |
| Gson                         | Serialización/deserialización JSON         | AGPL-3.0       | github.com/google/gson                                             | Copyright © Google LLC. All rights reserved.              |
| CommonMark (commonmark-java) | Parseador de Markdown                      | BSD 2-Clause     | github.com/commonmark/commonmark-java                              | Copyright © 2015-2022 Atlassian and others.               |
| Prism4j                      | Resaltado de sintaxis en bloques de código | MIT              | github.com/noties/Prism4j                                          | Copyright © 2019 Noties.                                  |
| JLatexMath                   | Renderizado de fórmulas LaTeX              | GPL-2.0-or-later | github.com/opencollab/jlatexmath Copyright © 2009-2022 OpenCollab. |
| WorkManager                  | Programación de tareas en segundo plano    | AGPL-3.0       | developer.android.com/topic/libraries/architecture/workmanager     | Copyright © Google LLC. All rights reserved.              |
| Material Icons               | Iconografía de la interfaz                 | AGPL-3.0       | fonts.google.com/icons                                             | Copyright © Google LLC. All rights reserved.              |
| Google Fonts (Roboto)        | Tipografías                                | SIL OFL 1.1      | fonts.google.com                                                   | Copyright © Google LLC. Licensed under Open Font License. |

### Backend (`Node.js`)

| Herramienta / Librería | Propósito                                      | Licencia | Enlace                           | Aviso de copyright                                       |
|------------------------|------------------------------------------------|----------|----------------------------------|----------------------------------------------------------|
| `Node.js`              | Entorno de ejecución JavaScript                | MIT      | nodejs.org                       | Copyright © OpenJS Foundation and contributors.          |
| `Express.js`           | Framework web                                  | MIT      | expressjs.com                    | Copyright © 2010-2024 StrongLoop, Inc. and contributors. |
| Helmet                 | Seguridad (cabeceras HTTP)                     | MIT      | github.com/helmetjs/helmet       | Copyright © 2012-2026 Evan Hahn, Adam Baldwin.           | 
| CORS                   | Middleware para Cross-Origin Resource Sharing  | MIT      | github.com/expressjs/cors        | Copyright © 2013-2024 Troy Goode and contributors.       |
| Compression            | Compresión de respuestas HTTP                  | MIT      | github.com/expressjs/compression | Copyright © 2014-2024 Jonathan Ong and contributors.     |
| Morgan                 | Logging de peticiones HTTP                     | MIT      | github.com/expressjs/morgan      | Copyright © 2014-2024 Jonathan Ong and contributors.     |
| dotenv                 | Carga de variables de entorno                  | MIT      | github.com/motdotla/dotenv       | Copyright © 2013 John Barton.                            |
| nodemon                | Reinicio automático del servidor en desarrollo | MIT      | github.com/remy/nodemon          | Copyright © Remy Sharp.                                  |

### Servicios en la nube

| Servicio   | Propósito                                    | Política de privacidad / Términos | Aviso                                                                                                                                  |
|------------|----------------------------------------------|-----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| Supabase   | Base de datos, autenticación, almacenamiento | supabase.com/privacy              | Copyright © 2020-2024 Supabase Inc. Todos los datos son propiedad de los usuarios y se gestionan conforme a su política de privacidad. |
| Render.com | Alojamiento del backend                      | render.com/privacy                | Copyright © 2024 Render Inc. Se utiliza como plataforma de despliegue del servidor API.                                                |

---

### Resumen de cumplimiento de licencias

| Licencia         | Proyectos que la usan                                                                 | Cumplimiento                                                                                                                                                                                                                                                                                          |
|------------------|---------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| MIT              | Express, Helmet, CORS, Compression, Morgan, dotenv, Prism4j, nodemon                  | Se mantiene el aviso de copyright en el código fuente y se incluye esta sección.                                                                                                                                                                                                                      |
| AGPL-3.0       | Android SDK, AndroidX, Material Components, OkHttp, Gson, WorkManager, Material Icons | Se incluye el aviso de copyright y la exención de responsabilidad en el código fuente.                                                                                                                                                                                                                |
| BSD 2-Clause     | Glide, CommonMark                                                                     | Se incluye el aviso de copyright en el código fuente y en esta sección.                                                                                                                                                                                                                               |
| GPL-2.0-or-later | JLatexMath                                                                            | Esta librería se utiliza en la aplicación final (APK). Su licencia GPL requiere que el código fuente completo de la aplicación se ponga a disposición de los usuarios. PreuSync no es de código abierto, pero el código fuente de JLatexMath está disponible públicamente en su repositorio oficial. |
| SIL OFL 1.1      | Google Fonts                                                                          | Se incluye el aviso de la licencia Open Font en el código fuente y en esta sección.                                                                                                                                                                                                                   |

---

### Aviso legal de terceros

Este producto incluye software desarrollado por terceros y distribuido bajo las licencias mencionadas anteriormente. A continuación se detallan los avisos legales correspondientes:

**Glide (BSD 2-Clause)**

```aviso
Copyright (c) 2014 Google, Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
```

**OkHttp (AGPL-3.0)**

```aviso
Copyright (C) 2019 Square, Inc.

Licensed under the GNU Affero General Public License v3 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

**CommonMark (BSD 2-Clause)**

```aviso
Copyright (c) 2015-2022 Atlassian and others.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
```

**JLatexMath (GPL-2.0-or-later)**

```aviso
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
```

---

### Nota sobre el cumplimiento de la GPL

PreuSync utiliza **JLatexMath**, una librería licenciada bajo **GPL-2.0-or-later**. Esta licencia requiere que el código fuente completo de la aplicación que la utiliza se ponga a disposición de los usuarios finales.

**PreuSync cumple con este requisito** al proporcionar acceso al código fuente de JLatexMath a través de su repositorio oficial:

https://github.com/opencollab/jlatexmath

El código fuente de PreuSync no es público, pero el código de JLatexMath está disponible para su descarga y estudio según los términos de la GPL.

---

### Contacto

- Web: binaryqva.com
- GitHub: JiroxDEV
- Email: soporte@binaryqva.com

---

> Nota: Esta aplicación se encuentra en fase de desarrollo activo. Algunas características pueden cambiar o estar en fase experimental. Agradecemos cualquier reporte de errores o sugerencia de mejora.

---

Hecho con ❤️ para la comunidad educativa.

**© 2025-2026 PreuSync. Todos los derechos reservados.**

