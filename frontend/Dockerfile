# Frontend
FROM node:19-alpine

WORKDIR /app

COPY package.json .
COPY tsconfig.json .
COPY tsconfig.app.json .
COPY tsconfig.node.json .

RUN npm install

COPY src ./src
COPY vite.config.ts .
COPY index.html .

RUN npm i -g serve

RUN npm run build

COPY . .

EXPOSE 3000

CMD ["serve", "-s", "dist"]