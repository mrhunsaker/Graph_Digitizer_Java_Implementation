#!/usr/bin/env bash
#
# versioning
#
# Purpose:
#   Automate version bumps, git tagging, and Javadoc publishing to the gh-pages branch.
#   Intended to streamline releasing new API documentation folders (e.g. /1.2/, /2.0/).
#
# Features:
#   - Bump Maven project version (pom.xml) using mvn versions:set (fallback to sed).
#   - Create and push annotated git tag (configurable prefix, default 'v').
#   - Build project & generate Javadoc (mvn clean install site).
#   - Stage Javadoc into gh-pages via a git worktree, invoking scripts/generate-javadoc-index.sh.
#   - Update CHANGELOG.md with a new version header if absent.
#   - Dry-run support to preview changes.
#
# Usage (examples):
#   ./versioning release --version 1.2
#   ./versioning bump --version 2.0.0 --no-tag
#   ./versioning docs --version 1.2        # Only (re)publish Javadoc for existing pom.xml version
#   ./versioning tag --version 1.2         # Just create/push tag (assumes pom already bumped)
#   ./versioning release --version 1.3 --dry-run
#
# Arguments:
#   Command (required): one of bump | tag | docs | release | help
#
#   --version <ver>     Target semantic version string (e.g. 1.2, 1.2.0, 2.0).
#   --no-tag            Skip creating a git tag (only valid with bump/release).
#   --tag-prefix <pfx>  Override tag prefix (default: v -> tag 'v1.2').
#   --worktree-dir <d>  Directory for gh-pages worktree (default: .gh-pages-workdir).
#   --skip-tests        Skip tests during build.
#   --dry-run           Show planned actions without performing writes/pushes.
#   --force             Do not prompt for confirmation.
#   --message <text>    Additional release note / tag annotation text.
#
# Environment Overrides:
#   TAG_PREFIX, WORKTREE_DIR, SITE_BASE, CHANGELOG_FILE
#
# Requirements:
#   - git >= 2.20 (worktree feature)
#   - Java & Maven installed
#   - scripts/generate-javadoc-index.sh present
#
# Exit Codes:
#   0 success
#   2 usage / validation error
#   3 operation failure
#
set -euo pipefail

# ---------------------------
# Defaults / Config
# ---------------------------
TAG_PREFIX="${TAG_PREFIX:-v}"
WORKTREE_DIR="${WORKTREE_DIR:-.gh-pages-workdir}"
CHANGELOG_FILE="${CHANGELOG_FILE:-CHANGELOG.md}"
GENERATE_SCRIPT="scripts/generate-javadoc-index.sh"
SITE_BASE="${SITE_BASE:-https://mrhunsaker.github.io/Graph_Digitizer_Java_Implementation}"
PROJECT_URL_DEFAULT="https://github.com/mrhunsaker/Graph_Digitizer_Java_Implementation"

# Runtime flags
COMMAND=""
VERSION=""
NO_TAG=0
SKIP_TESTS=0
DRY_RUN=0
FORCE=0
EXTRA_MESSAGE=""
# ---------------------------
# Helpers
# ---------------------------
info() { echo "[info] $*"; }
warn() { echo "[warn] $*" >&2; }
err()  { echo "[error] $*" >&2; exit 2; }
die()  { echo "[fatal] $*" >&2; exit 3; }

confirm() {
  [[ ${FORCE} -eq 1 ]] && return 0
  read -r -p "Proceed? [y/N] " ans
  [[ "${ans}" =~ ^[Yy]$ ]]
}

usage() {
  grep -E "^# " "$0" | sed 's/^# //'
  cat <<EOF

Commands:
  bump     Update pom.xml version (and optionally tag).
  tag      Create/push git tag only (pom.xml must already reflect version).
  docs     Build and publish Javadoc only (no version bump or tag).
  release  bump + build + docs + tag (full workflow).
  help     Show this help.

EOF
}

require_file() {
  [[ -f "$1" ]] || err "Required file missing: $1"
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || err "Required command '$1' not found in PATH"
}

# Extract current pom version (first occurrence under <project>)
read_pom_version() {
  awk '/<version>/ && !found { gsub(/.*<version>|<\/version>.*/, "", $0); print; found=1 }' pom.xml
}

# Perform version bump
bump_version() {
  local new_ver="$1"
  local current_ver
  current_ver="$(read_pom_version)"
  if [[ "${current_ver}" == "${new_ver}" ]]; then
    info "pom.xml already at version ${new_ver}"
    return 0
  fi

  info "Bumping version: ${current_ver} -> ${new_ver}"

  if [[ ${DRY_RUN} -eq 1 ]]; then
    info "[dry-run] Would run: mvn versions:set -DnewVersion=${new_ver} -DgenerateBackupPoms=false"
    return 0
  fi

  if command -v mvn >/dev/null 2>&1; then
    mvn -q versions:set -DnewVersion="${new_ver}" -DgenerateBackupPoms=false
  else
    err "Maven not found for versions:set operation"
  fi
}

# Update CHANGELOG with heading if absent
update_changelog() {
  local ver="$1"
  local date_str
  date_str="$(date -u +"%Y-%m-%d")"
  require_file "${CHANGELOG_FILE}"

  if grep -Eq "^## \\[${ver}\\]" "${CHANGELOG_FILE}"; then
    info "CHANGELOG already contains heading for ${ver}"
    return 0
  fi

  info "Adding new CHANGELOG heading for version ${ver}"
  if [[ ${DRY_RUN} -eq 1 ]]; then
    info "[dry-run] Would prepend heading to ${CHANGELOG_FILE}"
    return 0
  fi

  tmp_file="${CHANGELOG_FILE}.tmp.$$"
  {
    echo "## [${ver}] - ${date_str}"
    echo ""
    echo "- (placeholder) Describe changes here."
    echo ""
    cat "${CHANGELOG_FILE}"
  } > "${tmp_file}"
  mv "${tmp_file}" "${CHANGELOG_FILE}"
  info "CHANGELOG updated."
}

# Create and push tag
create_tag() {
  local ver="$1"
  local tag="${TAG_PREFIX}${ver}"

  if git rev-parse -q --verify "refs/tags/${tag}" >/dev/null 2>&1; then
    warn "Tag ${tag} already exists; skipping tag creation."
    return 0
  fi

  info "Creating git tag: ${tag}"

  if [[ ${DRY_RUN} -eq 1 ]]; then
    info "[dry-run] Would run: git tag -a ${tag} -m \"Release ${ver}\""
    return 0
  fi

  git add pom.xml "${CHANGELOG_FILE}" || true
  git commit -m "chore: release ${ver}" || warn "Nothing to commit before tagging."
  local annotation="Release ${ver}"
  [[ -n "${EXTRA_MESSAGE}" ]] && annotation="${annotation} - ${EXTRA_MESSAGE}"
  git tag -a "${tag}" -m "${annotation}"
  git push origin HEAD
  git push origin "${tag}"
  info "Tag pushed: ${tag}"
}

# Build Maven site (includes Javadoc)
build_site() {
  local mvn_args=(clean install site)
  [[ ${SKIP_TESTS} -eq 1 ]] && mvn_args+=(-DskipTests)
  info "Running Maven build: mvn ${mvn_args[*]}"
  if [[ ${DRY_RUN} -eq 1 ]]; then
    info "[dry-run] Would run mvn ${mvn_args[*]}"
    return 0
  fi
  mvn "${mvn_args[@]}"
}

# Prepare gh-pages worktree
ensure_worktree() {
  if [[ ! -d "${WORKTREE_DIR}" ]]; then
    info "Creating gh-pages worktree in ${WORKTREE_DIR}"
    if [[ ${DRY_RUN} -eq 1 ]]; then
      info "[dry-run] Would run: git worktree add ${WORKTREE_DIR} gh-pages (creating branch if missing)"
    else
      if ! git show-ref --verify --quiet refs/heads/gh-pages; then
        info "gh-pages branch missing; creating empty orphan branch"
        git checkout --orphan gh-pages
        rm -f .git/index
        git reset
        echo "<!-- gh-pages branch initialized -->" > .init.html
        git add .init.html
        git commit -m "Initialize gh-pages branch"
        git push origin gh-pages
        git checkout -
      fi
      git worktree add "${WORKTREE_DIR}" gh-pages
    fi
  else
    info "Reusing existing gh-pages worktree: ${WORKTREE_DIR}"
  fi
}

# Publish Javadoc
publish_javadoc() {
  local ver="$1"
  require_file "${GENERATE_SCRIPT}"

  ensure_worktree

  local apidocs_src="target/site/apidocs"
  [[ -d "${apidocs_src}" ]] || die "Javadoc source directory not found: ${apidocs_src}. Did you run 'mvn site'?"

  local staging="${WORKTREE_DIR}/apidocs-temp"

  info "Staging Javadoc into gh-pages worktree"
  if [[ ${DRY_RUN} -eq 1 ]]; then
    info "[dry-run] Would copy ${apidocs_src} -> ${staging}"
  else
    rm -rf "${staging}"
    cp -R "${apidocs_src}" "${staging}"
  fi

  local script_cmd=(bash "${GENERATE_SCRIPT}" --root "${WORKTREE_DIR}" --version "${ver}" --latest --project-url "${PROJECT_URL_DEFAULT}")
  info "Invoking generate-javadoc-index.sh"
  if [[ ${DRY_RUN} -eq 1 ]]; then
    info "[dry-run] Would run: ${script_cmd[*]}"
  else
    "${script_cmd[@]}"
  fi

  if [[ ${DRY_RUN} -eq 0 ]]; then
    pushd "${WORKTREE_DIR}" >/dev/null
      git add .
      git commit -m "docs: publish Javadoc ${ver}" || warn "No changes to commit in gh-pages."
      git push origin gh-pages
    popd >/dev/null
    info "Javadoc published for version ${ver}"
    info "Browse: ${SITE_BASE}/${ver}/index.html"
  else
    info "[dry-run] Would commit & push gh-pages changes."
  fi
}

# Validate version string
validate_version() {
  local ver="$1"
  [[ -z "${ver}" ]] && err "--version is required"
  if ! [[ "${ver}" =~ ^[0-9]+(\.[0-9]+){0,2}$ ]]; then
    warn "Version '${ver}' does not strictly match semantic X[.Y][.Z]; continuing anyway."
  fi
}

# ---------------------------
# Parse arguments
# ---------------------------
if [[ $# -eq 0 ]]; then usage; exit 2; fi

while [[ $# -gt 0 ]]; do
  case "$1" in
    bump|tag|docs|release|help)
      COMMAND="$1"; shift;;
    --version)
      VERSION="${2:-}"; shift 2;;
    --no-tag)
      NO_TAG=1; shift;;
    --tag-prefix)
      TAG_PREFIX="${2:-}"; shift 2;;
    --worktree-dir)
      WORKTREE_DIR="${2:-}"; shift 2;;
    --skip-tests)
      SKIP_TESTS=1; shift;;
    --dry-run)
      DRY_RUN=1; shift;;
    --force)
      FORCE=1; shift;;
    --message)
      EXTRA_MESSAGE="${2:-}"; shift 2;;
    --help|-h)
      usage; exit 0;;
    *)
      err "Unknown argument: $1"
      ;;
  esac
done

[[ "${COMMAND}" == "help" ]] && { usage; exit 0; }

# ---------------------------
# Pre-flight checks
# ---------------------------
require_file "pom.xml"
require_file "${GENERATE_SCRIPT}"
require_cmd git
require_cmd bash
require_cmd mvn

if [[ "${COMMAND}" != "docs" && "${COMMAND}" != "tag" ]]; then
  validate_version "${VERSION}"
fi

CURRENT_POM_VERSION="$(read_pom_version)"

# ---------------------------
# Plan summary
# ---------------------------
echo "--------------------------------------------------"
echo "Versioning Command Summary"
echo "  Command:        ${COMMAND}"
echo "  Target Version: ${VERSION:-N/A}"
echo "  Current pom:    ${CURRENT_POM_VERSION}"
echo "  Tag Prefix:     ${TAG_PREFIX}"
echo "  Worktree Dir:   ${WORKTREE_DIR}"
echo "  Skip Tests:     ${SKIP_TESTS}"
echo "  Dry Run:        ${DRY_RUN}"
echo "  Force:          ${FORCE}"
echo "  No Tag:         ${NO_TAG}"
echo "--------------------------------------------------"

# Confirm (unless help)
if ! confirm; then
  echo "Aborted."
  exit 1
fi

# ---------------------------
# Execute workflow
# ---------------------------
case "${COMMAND}" in
  bump)
    bump_version "${VERSION}"
    update_changelog "${VERSION}"
    if [[ ${NO_TAG} -eq 0 ]]; then
      create_tag "${VERSION}"
    else
      info "--no-tag specified; skipping tagging."
    fi
    ;;
  tag)
    [[ -z "${VERSION}" ]] && VERSION="${CURRENT_POM_VERSION}"
    validate_version "${VERSION}"
    create_tag "${VERSION}"
    ;;
  docs)
    # If --version omitted, use pom version
    [[ -z "${VERSION}" ]] && VERSION="${CURRENT_POM_VERSION}"
    build_site
    publish_javadoc "${VERSION}"
    ;;
  release)
    bump_version "${VERSION}"
    update_changelog "${VERSION}"
    build_site
    publish_javadoc "${VERSION}"
    if [[ ${NO_TAG} -eq 0 ]]; then
      create_tag "${VERSION}"
    else
      info "--no-tag specified; skipping tagging."
    fi
    ;;
  *)
    err "Unhandled command: ${COMMAND}"
    ;;
esac

info "Done."
