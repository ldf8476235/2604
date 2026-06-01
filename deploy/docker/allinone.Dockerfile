FROM docker.m.daocloud.io/library/nginx:1.27-alpine

ENV TZ=Asia/Shanghai
WORKDIR /app

RUN apk add --no-cache openjdk8-jre curl tzdata \
    && mkdir -p /run/nginx

COPY backend/platform-0.1.0.jar /app/platform.jar
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY web /usr/share/nginx/html
COPY admin /usr/share/nginx/html/beifanghanzi
COPY studio /usr/share/nginx/html/studio
COPY allinone/start.sh /app/start.sh
RUN chmod +x /app/start.sh

EXPOSE 80
CMD ["/app/start.sh"]
