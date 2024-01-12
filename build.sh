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
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "yarn install --no-bin-links && node_modules/gulp/bin/gulp.js build && yarn run build:sass"
    ;;
  *)
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "yarn install && node_modules/gulp/bin/gulp.js build && yarn run build:sass"
    ;;
  esac
}

test() {
  rm -rf coverage
  rm -rf */build
  case $(uname -s) in
  MINGW*)
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "yarn install --no-bin-links && node_modules/gulp/bin/gulp.js drop-cache &&  yarn test"
    ;;
  *)
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "yarn install && node_modules/gulp/bin/gulp.js drop-cache && yarn test"
    ;;
  esac
}

testNode () {
  rm -rf coverage
  rm -rf */build
  case `uname -s` in
    MINGW*)
      docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "yarn install --no-bin-links && node_modules/gulp/bin/gulp.js drop-cache && yarn test"
      ;;
    *)
      docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "yarn install && node_modules/gulp/bin/gulp.js drop-cache && yarn test"
  esac
}

testNodeDev () {
  rm -rf coverage
  rm -rf */build
  case `uname -s` in
    MINGW*)
      docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "yarn install --no-bin-links && node_modules/gulp/bin/gulp.js drop-cache && yarn run test:dev"
      ;;
    *)
      docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "yarn install && node_modules/gulp/bin/gulp.js drop-cache && yarn run test:dev"
  esac
}

testGradle() {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle test --no-build-cache --rerun-tasks
}

buildGradle() {
  docker-compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle shadowJar install publishToMavenLocal
}

buildGulp() {
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "yarn install --no-bin-links && node_modules/gulp/bin/gulp.js build"
}

buildCss() {
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "yarn run build:sass"
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
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "yarn install --no-bin-links && node_modules/gulp/bin/gulp.js build --targetModule=presences && yarn run build:sass"
    ;;
  *)
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "yarn install && node_modules/gulp/bin/gulp.js build --targetModule=presences && yarn run build:sass"
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
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "yarn install --no-bin-links && node_modules/gulp/bin/gulp.js build --targetModule=incidents && yarn run build:sass"
    ;;
  *)
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "yarn install && node_modules/gulp/bin/gulp.js build --targetModule=incidents && yarn run build:sass"
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
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "yarn install --no-bin-links && node_modules/gulp/bin/gulp.js build --targetModule=massmailing && yarn run build:sass"
    ;;
  *)
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "yarn install && node_modules/gulp/bin/gulp.js build --targetModule=massmailing && yarn run build:sass"
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
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "yarn install --no-bin-links && node_modules/gulp/bin/gulp.js build --targetModule=statistics-presences && yarn run build:sass"
    ;;
  *)
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "yarn install && node_modules/gulp/bin/gulp.js build --targetModule=statistics-presences && yarn run build:sass"
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
  buildGulp)
    buildGulp
    ;;
  buildCss)
    buildCss
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
