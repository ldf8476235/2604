FROM nginx:1.27-alpine
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY web /usr/share/nginx/html
COPY admin /usr/share/nginx/html/beifanghanzi
COPY studio /usr/share/nginx/html/studio
EXPOSE 80
