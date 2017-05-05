datasrc.sweeper.files <- function(dir) {
  list.files(dir, pattern = "catcluster.*.profile.sweeper.log", recursive = TRUE, full.names = TRUE)
}

datasrc.sweeper.load.one <-function(file) {
  fread(
    file,
    col.names = c('time', 'member', 'sweepables', 'sweeped', 'elapsed.sec')
  ) %>%
    mutate(
      time = parse_date_time2(time, "%Y-%m-%d %H:%M:%S"),
      member = factor(member)
    )
}

datasrc.sweeper.load <- function(dir) {
  rbindlist(lapply(datasrc.sweeper.files(dir), datasrc.sweeper.load.one))
}
