FROM node:18-alpine

WORKDIR /app

ENV NODE_OPTIONS=--openssl-legacy-provider


# Copy package.json and package-lock.json
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy the rest of the application code
COPY . .

# Expose port 3000
EXPOSE 3000

# Start the React application
CMD ["npm", "start"]
