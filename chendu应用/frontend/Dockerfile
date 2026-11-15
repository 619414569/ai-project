# 前端在本地构建（npm install && npm run build），镜像只负责托管 dist：
#   cd frontend && npm install && npm run build   -> dist/
#   docker build -t notebook-frontend:local ./frontend
# 镜像内不再安装 Node，也不执行 npm，服务器无需 Node 环境。
FROM nginx:1.27-alpine
ENV BACKEND_URL=http://notebook-backend:8080
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY dist /usr/share/nginx/html
EXPOSE 80
