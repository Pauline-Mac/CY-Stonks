# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:17-jdk-jammy

# Set the working directory
WORKDIR /app

# Install sbt (Scala Build Tool)
RUN apt-get update && apt-get install -y apt-transport-https curl gnupg
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list
RUN echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add
RUN apt-get update && apt-get install -y sbt

# Copy the build.sbt and project files
COPY build.sbt .
COPY project project

# Copy the source code
COPY src src

# Run sbt to compile and run the project
CMD ["sbt", "runMain com.cystonks.Main"]