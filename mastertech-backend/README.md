# Master Tech — Backend (Spring Boot)

API REST para la tienda Master Tech (antes "Phantom"). Java 17 · Spring Boot · PostgreSQL (Supabase) · JWT · Stripe.

## Variables de entorno
Copia `.env.example` y configura cada variable en IntelliJ (Run → Edit Configurations → Environment variables) o en Render.
`JWT_SECRET` es obligatoria (mínimo 32 caracteres).

## Ejecutar
```bash
./gradlew bootRun        # http://localhost:8089
```

## Endpoints
| Método | Ruta | Acceso |
|---|---|---|
| POST | `/api/auth/login` · `/api/auth/registro` | público |
| GET | `/api/auth/me` | sesión |
| GET | `/api/v1/productos` · `/{id}` · `/keywords` | público |
| POST/PUT/DELETE | `/api/v1/productos` · `/{id}` | ADMIN |
| POST | `/api/v1/productos/{id}/imagen` (multipart `imagen`) | ADMIN |
| GET | `/api/v1/categorias` · `/{id}` | público |
| POST/PUT/DELETE | `/api/v1/categorias` | ADMIN |
| GET | `/api/v1/ventas` · `/{id}` | ADMIN |
| GET/POST/PUT/PATCH/DELETE | `/api/v1/usuarios` | ADMIN |
| GET/PUT | `/api/v1/me` · PUT `/api/v1/me/password` · GET `/api/v1/me/pedidos` | sesión |
| POST | `/api/v1/pedidos/checkout` · `/api/v1/pedidos/confirmar` | sesión |

Roles: `USER` (cliente) y `ADMIN`. CORS permitido para `FRONTEND_URL` (por defecto `http://localhost:5173`).
