.PHONY: up down logs ps build backend-jar backend-image frontend-dist config backend-test frontend-build

# 后端镜像依赖预构建 jar，前端镜像依赖本地构建的 dist
up: backend-jar backend-image frontend-dist
	docker compose up -d --build

down:
	docker compose down

logs:
	docker compose logs -f notebook-backend notebook-frontend

ps:
	docker compose ps

build: backend-jar backend-image frontend-dist
	docker compose build frontend

backend-jar:
	cd backend && mvn -B clean package

backend-image:
	docker build -t notebook-backend:local ./backend

frontend-dist:
	cd frontend && npm install && npm run build

config:
	docker compose config

backend-test:
	cd backend && mvn -B test

frontend-build:
	cd frontend && npm install && npm run build
