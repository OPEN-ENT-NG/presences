#!/bin/bash

if [ ! -e node_modules ]; then
  mkdir node_modules
fi

case $(uname -s) in
MINGW*)
  USER_UID=1000
  GROUP_UID=1000
  ;;
*)
  if [ -z ${USER_UID:+x} ]; then
    USER_UID=$(id -u)
    GROUP_GID=$(id -g)
  fi
  ;;
esac

clean() {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle clean
}

buildNode() {
  case $(uname -s) in
  MINGW*)
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install --no-bin-links && node_modules/gulp/bin/gulp.js build"
    ;;
  *)
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install && node_modules/gulp/bin/gulp.js build"
    ;;
  esac
}

test() {
  rm -rf coverage
  rm -rf */build
  case $(uname -s) in
  MINGW*)
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install --no-bin-links && node_modules/gulp/bin/gulp.js drop-cache &&  npm test"
    ;;
  *)
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install && node_modules/gulp/bin/gulp.js drop-cache && npm test"
    ;;
  esac
}

testNode () {
  rm -rf coverage
  rm -rf */build
  case `uname -s` in
    MINGW*)
      docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install --no-bin-links && node_modules/gulp/bin/gulp.js drop-cache && npm test"
      ;;
    *)
      docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install && node_modules/gulp/bin/gulp.js drop-cache && npm test"
  esac
}

testNodeDev () {
  rm -rf coverage
  rm -rf */build
  case `uname -s` in
    MINGW*)
      docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install --no-bin-links && node_modules/gulp/bin/gulp.js drop-cache && npm run test:dev"
      ;;
    *)
      docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install && node_modules/gulp/bin/gulp.js drop-cache && npm run test:dev"
  esac
}

testGradle() {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle test --no-build-cache --rerun-tasks
}

buildGradle() {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle shadowJar install publishToMavenLocal
}

publish() {
  if [ -e "?/.gradle" ] && [ ! -e "?/.gradle/gradle.properties" ]; then
    echo "odeUsername=$NEXUS_ODE_USERNAME" >"?/.gradle/gradle.properties"
    echo "odePassword=$NEXUS_ODE_PASSWORD" >>"?/.gradle/gradle.properties"
    echo "sonatypeUsername=$NEXUS_SONATYPE_USERNAME" >>"?/.gradle/gradle.properties"
    echo "sonatypePassword=$NEXUS_SONATYPE_PASSWORD" >>"?/.gradle/gradle.properties"
  fi
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle publish
}

presences() {
  case $(uname -s) in
  MINGW*)
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install --no-bin-links && node_modules/gulp/bin/gulp.js build --targetModule=presences"
    ;;
  *)
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install && node_modules/gulp/bin/gulp.js build --targetModule=presences"
    ;;
  esac
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle :presences:shadowJar :presences:install :presences:publishToMavenLocal
}

presences:buildGradle() {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle :presences:shadowJar :presences:install :presences:publishToMavenLocal
}

incidents() {
  case $(uname -s) in
  MINGW*)
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install --no-bin-links && node_modules/gulp/bin/gulp.js build --targetModule=incidents"
    ;;
  *)
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install && node_modules/gulp/bin/gulp.js build --targetModule=incidents"
    ;;
  esac
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle :incidents:shadowJar :incidents:install :incidents:publishToMavenLocal
}

incidents:buildGradle() {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle :incidents:shadowJar :incidents:install :incidents:publishToMavenLocal
}

massmailing() {
  case $(uname -s) in
  MINGW*)
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install --no-bin-links && node_modules/gulp/bin/gulp.js build --targetModule=massmailing"
    ;;
  *)
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install && node_modules/gulp/bin/gulp.js build --targetModule=massmailing"
    ;;
  esac
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle :massmailing:shadowJar :massmailing:install :massmailing:publishToMavenLocal
}

massmailing:buildGradle() {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle :massmailing:shadowJar :massmailing:install :massmailing:publishToMavenLocal
}

statistics() {
  case $(uname -s) in
  MINGW*)
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install --no-bin-links && node_modules/gulp/bin/gulp.js build --targetModule=statistics-presences"
    ;;
  *)
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install && node_modules/gulp/bin/gulp.js build --targetModule=statistics-presences"
    ;;
  esac
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle :statistics:shadowJar :statistics:install :statistics:publishToMavenLocal
}

statistics:buildGradle() {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle :statistics:shadowJar :statistics:install :statistics:publishToMavenLocal
}

for param in "$@"; do
  case $param in
  clean)
    clean
    ;;
  buildNode)
    buildNode
    ;;
  buildGradle)
    buildGradle
    ;;
  install)
    buildNode && buildGradle
    ;;
  publish)
    publish
    ;;
  test)
    testNode ; testGradle
    ;;
    testNode)
      testNode
    ;;
    testNodeDev)
      testNodeDev
    ;;
    testGradle)
      testGradle
      ;;
  presences)
    presences
    ;;
  incidents)
    incidents
    ;;
  massmailing)
    massmailing
    ;;
  statistics)
    statistics
    ;;
  presences:buildGradle)
    presences:buildGradle
    ;;
  incidents:buildGradle)
    incidents:buildGradle
    ;;
  massmailing:buildGradle)
    massmailing:buildGradle
    ;;
  statistics:buildGradle)
    statistics:buildGradle
    ;;
  *)
    echo "Invalid argument : $param"
    ;;
  esac
  if [ ! $? -eq 0 ]; then
    exit 1
  fi
done
