package com.sentinel.loadtest

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

import scala.concurrent.duration._

class SentinelLoadTest extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .authorizationHeader("Bearer test_token")

  val scn = scenario("Sentinel Load Test")
    .exec(http("Health Check")
      .get("/actuator/health"))
    .pause(1)
    .exec(http("Users Endpoint")
      .get("/api/v1/users"))
    .pause(1)
    .exec(http("Products Endpoint")
      .get("/api/v1/products"))

  setUp(
    scn.inject(
      rampUsers(10) during (5 seconds),
      constantUsersPerSec(5) during (10 seconds)
    )
  ).protocols(httpProtocol)
}