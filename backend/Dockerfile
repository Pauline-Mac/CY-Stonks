# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk-alpine

# Install necessary packages
RUN apk add --no-cache bash curl

# Download and install SBT
RUN curl -L -o sbt-1.5.5.tgz https://github.com/sbt/sbt/releases/download/v1.5.5/sbt-1.5.5.tgz && \
    tar xzf sbt-1.5.5.tgz -C /usr/local && \
    ln -s /usr/local/sbt/bin/sbt /usr/local/bin/sbt && \
    rm sbt-1.5.5.tgz

# Verify SBT installation
RUN sbt --version

# Set the working directory
WORKDIR /app

# Copy build.sbt and project files
COPY build.sbt /app/
COPY project /app/project/

# Download dependencies (this will cache the dependencies layer)
RUN sbt update

# Copy the rest of the application code
COPY src /app/src/

# Expose the necessary port
EXPOSE 9000

# Command to run the application
CMD ["sbt", "runMain Main"]