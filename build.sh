#!/bin/bash

MVN_OPTS="-Duser.home=/var/maven -T 4"


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

clean () {
  echo "Cleaning front files"
  rm -rf ./presences/src/main/resources/public/dist
  rm -rf ./presences/src/main/resources/public/js
  rm -rf ./presences/src/main/resources/public/build
  if [ "$NO_DOCKER" = "true" ] ; then
    mvn clean
  else
    docker compose run --rm maven mvn $MVN_OPTS clean
  fi
}


# Build frontend des 4 modules migrés sur Vite (presences, incidents, statistics-presences, massmailing).
buildFrontend () {
  echo "Running pnpm install..."
  if [ "$NO_DOCKER" = "true" ] ; then
    pnpm install
  else
    docker compose run -e NPM_TOKEN --rm -u "$USER_UID:$GROUP_GID" node sh -c "pnpm install"
  fi

  VERSION=$(date +%s)

  for module in presences incidents statistics-presences massmailing; do
    if [ ! -e "./$module/src/main/resources/view" ] ; then
      mkdir "./$module/src/main/resources/view"
    fi
    find "./$module/src/main/resources/view-src" -type f \( -name "*.html" -o -name "*.json" \) | while read -r file; do
      dest="./$module/src/main/resources/view/${file#./$module/src/main/resources/view-src/}"
      mkdir -p "$(dirname "$dest")"
      sed "s/@@VERSION/$VERSION/g" "$file" > "$dest"
    done
    echo "Building frontend $module..."
    if [ "$NO_DOCKER" = "true" ] ; then
      pnpm run "build:$module"
    else
      docker compose run -e NPM_TOKEN --rm -u "$USER_UID:$GROUP_GID" node sh -c "pnpm run build:$module"
    fi
    status=$?
    if [ $status != 0 ] ; then
      exit $status
    fi
  done
}

testNode () {
  rm -rf coverage
  rm -rf */build
  case `uname -s` in
    MINGW*)
      docker compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "pnpm install --no-bin-links && pnpm test"
      ;;
    *)
      docker compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "pnpm install && pnpm test"
  esac
}

testNodeDev () {
  rm -rf coverage
  rm -rf */build
  case `uname -s` in
    MINGW*)
      docker compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "pnpm install --no-bin-links && pnpm run test:dev"
      ;;
    *)
      docker compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "pnpm install && pnpm run test:dev"
  esac
}

test () {
  docker compose run --rm maven mvn $MVN_OPTS test
}


install () {
  docker compose run --rm maven mvn $MVN_OPTS clean install -U -DskipTests
}

publish() {
  version=`docker compose run --rm maven mvn $MVN_OPTS help:evaluate -Dexpression=project.version -q -DforceStdout`
  level=`echo $version | cut -d'-' -f3`
  case "$level" in
    *SNAPSHOT) export nexusRepository='snapshots' ;;
    *)         export nexusRepository='releases' ;;
  esac
  docker compose run --rm  maven mvn $MVN_OPTS -DrepositoryId=ode-$nexusRepository -DskipTests -Dmaven.test.skip=true --settings /var/maven/.m2/settings.xml deploy
}

presences() {
  buildFrontend
  docker compose run --rm maven mvn $MVN_OPTS -pl presences -am install -DskipTests
}

presences:buildMaven() {
  docker compose run --rm maven mvn $MVN_OPTS -pl presences -am install -DskipTests
}

incidents() {
  if [ "$NO_DOCKER" = "true" ] ; then
    pnpm install && pnpm run build:incidents
  else
    docker compose run -e NPM_TOKEN --rm -u "$USER_UID:$GROUP_GID" node sh -c "pnpm install && pnpm run build:incidents"
  fi
  docker compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle :incidents:shadowJar :incidents:install :incidents:publishToMavenLocal
}

incidents:buildGradle() {
  docker compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle :incidents:shadowJar :incidents:install :incidents:publishToMavenLocal
}

massmailing() {
  if [ "$NO_DOCKER" = "true" ] ; then
    pnpm install && pnpm run build:massmailing
  else
    docker compose run -e NPM_TOKEN --rm -u "$USER_UID:$GROUP_GID" node sh -c "pnpm install && pnpm run build:massmailing"
  fi
  docker compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle :massmailing:shadowJar :massmailing:install :massmailing:publishToMavenLocal
}

massmailing:buildGradle() {
  docker compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle :massmailing:shadowJar :massmailing:install :massmailing:publishToMavenLocal
}

statistics() {
  if [ "$NO_DOCKER" = "true" ] ; then
    pnpm install && pnpm run build:statistics-presences
  else
    docker compose run -e NPM_TOKEN --rm -u "$USER_UID:$GROUP_GID" node sh -c "pnpm install && pnpm run build:statistics-presences"
  fi
  docker compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle :statistics:shadowJar :statistics:install :statistics:publishToMavenLocal
}

statistics:buildGradle() {
  docker compose run --rm -u "$USER_UID:$GROUP_GID" gradle gradle :statistics:shadowJar :statistics:install :statistics:publishToMavenLocal
}

publishNexus() {
  version=`docker compose run --rm maven mvn $MVN_OPTS help:evaluate -Dexpression=project.version -q -DforceStdout`
  level=`echo $version | cut -d'-' -f3`
  case "$level" in
    *SNAPSHOT) export nexusRepository='snapshots' ;;
    *)         export nexusRepository='releases' ;;
  esac
  docker compose run --rm  maven mvn $MVN_OPTS -DrepositoryId=ode-$nexusRepository -Durl=$repo -DskipTests -Dmaven.test.skip=true --settings /var/maven/.m2/settings.xml deploy
}

# If DEBUG env var is set to "true" then set -x to enable debug mode
if [ "$DEBUG" == "true" ]; then
	set -x
	EDIFICE_CLI_DEBUG_OPTION="--debug"
else
	EDIFICE_CLI_DEBUG_OPTION=""
fi

init() {
  me=`id -u`:`id -g`
  echo "DEFAULT_DOCKER_USER=$me" > .env

  # If CLI_VERSION is empty set to latest
  if [ -z "$CLI_VERSION" ]; then
    CLI_VERSION="latest"
  fi
  curl -sfL https://maven.opendigitaleducation.com/repository/releases/io/edifice/edifice-cli/install.sh | TARGET_DIR=. EDIFICE_CLI_VERSION=$CLI_VERSION bash
  ./edifice version $EDIFICE_CLI_DEBUG_OPTION
}

if [ ! -e .env ]; then
  init
fi

for param in "$@"; do
  case $param in
  init)
    init
    ;;
  clean)
    clean
    ;;
  buildFrontend)
    buildFrontend
    ;;
  buildMaven)
    install
    ;;
  install)
    buildFrontend && install
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
