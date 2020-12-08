# Hivestar-ORTB

OpenRTB adapter for Hivestack and Vistar SSPs.  This adapter receives an OpenRTB bid request, translates it into Hivestack / Vistar ad requests, receives the responses from those SSPs and translates the responses back to an OpenRTB bid response.

## Integration with Broadsign Reach

In order to facilitate integration of this adapter into Broadsign Reach, hivestar-ortb will automatically scan for approved creatives in Hivestack and Vistar and ingest them into Reach for distribution and caching. It also implements several Reach-specific callbacks for troubleshooting such as win/loss notifications.

## Building

Set up project defaults in `src/main/resources/application.yml` -- note that all
properties can be overriden at runtime via Spring Boot's [externalized configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config) system.

You'll need to define appropriate API keys and a mysql JDBC data source. 

Project uses Maven build system, build defined by `pom.xml`. Dockerfile provided as well.

Docker build with `docker build -t starproxy/starproxy .`

Maven build with `./mvnw package`

## Running

Define environment variables to override default configuration as desired.

Run with (for example...) 
```
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/starproxy
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=something
export SERVER_PORT=8080
./mvnw spring-boot:run
```

or

Run with `docker run -p 8080:8080 starproxy/starproxy`

Application should accept HTTP connections on specfied port, ready to receive ortb bid requests.

Paths available are `/bids/hivestack`, `/bids/vistar` and `/bids/vistar_french`.  
Also provides `/cachedDocuments` and `/win` and `/loss` for callbacks from Reach.

Application uses [Thumbor](http://thumbor.org) to convert PNG creative to JPG. A Thumbor instance must be set up
and configured in the yml for this funcitonality to work.

## Editing/Developing

This codebase uses [Spring Boot](https://spring.io/projects/spring-boot) to provide many services in a standard way. 
Significant reconfiguration can be done by changing spring boot properties.

This codebase uses [Project Lombok](https://projectlombok.org) to reduce Java boilerplate. Using an IDE that supports Lombok
will make your life much more tolerable. There are plugins for Eclipse and IntelliJ IDEA. 

## Database Schema (for MySQL)

```
USE starproxy;

CREATE TABLE `playlogs` (
    `id` int(11) UNSIGNED NOT NULL,
    `generator_id` varchar(255) DEFAULT NULL,
    `vistar_enabled` enum('N','Y') NOT NULL DEFAULT 'N',
    `vistar_language` enum('EN','FR') NOT NULL DEFAULT 'EN',
    `reach_device_ifa` varchar(255) DEFAULT NULL,
    `rott_ad_width` int(10) UNSIGNED DEFAULT NULL,
    `rott_ad_height` int(10) UNSIGNED DEFAULT NULL,
    `hivestack_enabled` enum('N','Y') NOT NULL DEFAULT 'N',
    `hivestack_display_uuid` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `playlogs`
    ADD PRIMARY KEY (`id`),
    ADD UNIQUE KEY `generator_id` (`generator_id`),
    ADD UNIQUE KEY `reach_device_ifa` (`reach_device_ifa`),
    ADD UNIQUE KEY `hivestack_display_uuid` (`hivestack_display_uuid`);

ALTER TABLE `playlogs`    MODIFY `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT;

CREATE TABLE `creatives` (
     `id` int(6) NOT NULL,
     `hivestack_url` varchar(255) NOT NULL,
     `reach_id` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `creatives`
    ADD PRIMARY KEY (`id`),
    ADD UNIQUE KEY `hivestack_url` (`hivestack_url`);

ALTER TABLE `creatives`
    MODIFY `id` int(6) NOT NULL AUTO_INCREMENT;

```
