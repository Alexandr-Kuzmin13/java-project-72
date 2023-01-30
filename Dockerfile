FROM gradle:7.4-jdk17

WORKDIR /app

COPY . /app

RUN gradle installDist

CMD ./build/install/java-project-72/bin/java-project-72