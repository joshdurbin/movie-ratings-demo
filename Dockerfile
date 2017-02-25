FROM gcr.io/google_appengine/openjdk8
LABEL maintainer "durbinjo593@gmail.com"
WORKDIR /opt/movie-ratings-demo
ADD build/libs/movie-ratings-demo-all.jar movie-ratings-demo-all.jar
CMD ["java", "-jar", "/opt/movie-ratings-demo/movie-ratings-demo-all.jar", "-Dratpack.port=8080", "-Dratingsdemo.mongoURI=mongodb://mongo/movie-ratings?w=1", "-Dratingsdemo.redisURI=redis://redis"]
EXPOSE 8080
