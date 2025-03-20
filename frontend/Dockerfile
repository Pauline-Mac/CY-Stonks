# Frontend
FROM node:19-alpine

WORKDIR /app

COPY package.json .

COPY tsconfig.json .

RUN npm install

RUN npm i -g serve

COPY . .

RUN npm run build

EXPOSE 3000

CMD [ "serve", "-s", "dist" ]

# Ensure the dist directory is correctly served
RUN mkdir -p /app/dist