.PHONY: up down logs ps build backend-test frontend-build

up:
	docker compose up -d --build

down:
	docker compose down

logs:
	docker compose logs -f backend frontend

ps:
	docker compose ps

build:
	docker compose build

backend-test:
	cd backend && mvn test

frontend-build:
	cd frontend && npm install && npm run build
