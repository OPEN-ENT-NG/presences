#!/bin/bash

MVN_OPTS="-Duser.home=/var/maven"


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

init() {
  me=`id -u`:`id -g`
  echo "DEFAULT_DOCKER_USER=$me" > .env
}

clean () {
  docker-compose run --rm maven mvn $MVN_OPT clean
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

test () {
  docker compose run --rm maven mvn $MVN_OPTS test
}


install () {
  docker compose run --rm maven mvn $MVN_OPTS clean install -U -DskipTests
}

buildGulp() {
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "yarn install --no-bin-links && node_modules/gulp/bin/gulp.js build"
}

buildCss() {
    docker-compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "yarn run build:sass"
}

publish() {
  version=`docker compose run --rm maven mvn $MVN_OPTS help:evaluate -Dexpression=project.version -q -DforceStdout`
  level=`echo $version | cut -d'-' -f3`
  case "$level" in
    *SNAPSHOT) export nexusRepository='snapshots' ;;
    *)         export nexusRepository='releases' ;;
  esac
  docker compose run --rm  maven mvn -DrepositoryId=ode-$nexusRepository -DskipTests -Dmaven.test.skip=true --settings /var/maven/.m2/settings.xml deploy
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
  docker-compose run --rm maven mvn $MVN_OPTS -pl presences -am install -DskipTests

}

presences:buildMaven() {
  docker-compose run --rm maven mvn $MVN_OPTS -pl presences -am install -DskipTests
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

publishNexus() {
  version=`docker compose run --rm maven mvn $MVN_OPTS help:evaluate -Dexpression=project.version -q -DforceStdout`
  level=`echo $version | cut -d'-' -f3`
  case "$level" in
    *SNAPSHOT) export nexusRepository='snapshots' ;;
    *)         export nexusRepository='releases' ;;
  esac
  docker compose run --rm  maven mvn -DrepositoryId=ode-$nexusRepository -Durl=$repo -DskipTests -Dmaven.test.skip=true --settings /var/maven/.m2/settings.xml deploy
}

for param in "$@"; do
  case $param in
  init)
    init
    ;;
  clean)
    clean
    ;;
  buildNode)
    buildNode
    ;;
  buildMaven)
    install
    ;;
  buildGulp)
    buildGulp
    ;;
  buildCss)
    buildCss
    ;;
  install)
    buildNode && install
    ;;
  publish)
    publish
    ;;
  publishNexus)
    publishNexus
    ;;
  test)
    testNode ; test
    ;;
  testNode)
    testNode
  ;;
  testNodeDev)
    testNodeDev
  ;;
  testMaven)
    test
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
  presences:buildMaven)
    presences:buildMaven
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
