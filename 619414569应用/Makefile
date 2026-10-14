.PHONY: deploy up down logs ps build backend-test frontend-build

deploy:
	sh scripts/deploy.sh

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
	cd frontend && npm ci && npm run build
