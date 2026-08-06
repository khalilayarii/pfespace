# ---------- ÉTAPE 1 : BUILD ----------
FROM node:18-alpine AS build

WORKDIR /app

# On copie d'abord package.json seul pour profiter du cache Docker
COPY package.json package-lock.json ./
RUN npm ci --legacy-peer-deps

# Puis le reste du code source
COPY . .

# Build en mode production
RUN npm run build -- --configuration production

# ---------- ÉTAPE 2 : RUN ----------
FROM nginx:alpine

# On copie les fichiers statiques générés vers le dossier servi par nginx
COPY --from=build /app/dist/pfeespace-frontend /usr/share/nginx/html

# Config nginx personnalisée (fichier créé juste après)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]