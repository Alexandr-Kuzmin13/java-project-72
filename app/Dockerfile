FROM gradle:7.4-jdk17

WORKDIR /

COPY . /app

RUN gradle installDist

CMD ./build/install/app/bin/app
