datasrc.cat.files <- function(dir) {
  list.files(dir, pattern = "catcluster.*.profile.log", recursive = TRUE, full.names = TRUE)
}

datasrc.cat.load.one <-function(file) {
  fread(
    file,
    col.names = c('time', 'member', 'remember.entities', 'num.entities', 'mem.used.redis', 'mem.used', 'mem.total', 'mem.free', 'mem.max')
  ) %>%
    mutate(
      time = parse_date_time2(time, "%Y-%m-%d %H:%M:%S"),
      member = factor(member)
    )
}

datasrc.cat.load <- function(dir) {
  rbindlist(lapply(datasrc.cat.files(dir), datasrc.cat.load.one))
}
