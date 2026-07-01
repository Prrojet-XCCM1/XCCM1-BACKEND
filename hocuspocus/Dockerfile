FROM node:20-alpine

WORKDIR /app

COPY package.json ./
RUN npm install --production && npm cache clean --force

COPY server.js ./

EXPOSE 1234

USER node

CMD ["node", "server.js"]
