# Movie Streaming

Du an da duoc tai cau truc theo 2 phan ro rang:

- `backend/`: Spring Boot API (Java)
- `frontend/`: giao dien web (HTML/CSS/JavaScript)

## Cau truc

- `backend/pom.xml`, `backend/src/...`: ma nguon backend
- `frontend/index.html`, `frontend/movie.html`, `frontend/favorites.html`: ma nguon frontend

## Chay backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend mac dinh: `http://localhost:8080`

## Chay frontend

Mo `frontend/index.html` bang Live Server (hoac static server bat ky).

Frontend duoc cau hinh goi API qua:

- `http://localhost:8080` (mac dinh)
- co the override bang `window.API_BASE` neu can.
