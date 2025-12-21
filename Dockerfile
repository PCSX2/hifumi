# ===== BUILD STAGE =====
FROM maven:3-amazoncorretto-25 AS build_stage

WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY . .
RUN mvn -B package

# Normalize jar name
RUN mv target/HifumiBot*.jar /build/hifumi.jar

# ===== FINAL STAGE =====
FROM amazoncorretto:25-alpine AS final_stage

# Update Alpine security packages
RUN apk update && apk upgrade --no-cache

# Needed for awt font stuff
RUN apk add --no-cache msttcorefonts-installer fontconfig
RUN update-ms-fonts

# Create non-root user
RUN addgroup -S app && adduser -S app -G app

WORKDIR /opt

COPY --from=build_stage /build/hifumi.jar ./hifumi.jar

USER app

CMD ["java", "-XX:MaxRAMPercentage=80", "-jar", "/opt/hifumi.jar"]
