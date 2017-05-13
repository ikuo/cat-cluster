sweeper.files <- function(dir) {
  list.files(dir, pattern = "catcluster.*.profile.sweeper.log", recursive = TRUE, full.names = TRUE)
}

sweeper.load.one <-function(file) {
  fread(
    file,
    col.names =
      c('time', 'member', 'iteration',
        'sweepables', 'sweeped', 'errors', 'elapsed.sec')
  ) %>%
    mutate(
      time = parse_date_time2(time, "%Y-%m-%d %H:%M:%S"),
      member = factor(member),
      iteration = factor(dense_rank(iteration))
    )
}

sweeper.load <- function(dir) {
  rbindlist(lapply(sweeper.files(dir), sweeper.load.one))
}

sweeper.bases <- function(df) {
  df %>%
    mutate(base_time = floor(as.numeric(time) / 5)) %>%
    group_by(base_time) %>%
    summarize(
      n = n(),
      entities = quantile(entities, .95),
      mem.used = sum(mem.used),
      mem.used.redis = median(mem.used.redis)
    )
}

sweeper.overview <- function(df) {
  plots <- list(
    ggplot(df, aes(x = time, y = elapsed.sec, color = iteration)) + geom_point(),
    ggplot(df, aes(x = time, y = sweepables, color = iteration)) + geom_point(),
    ggplot(df, aes(x = time, y = sweeped, color = iteration)) + geom_point(),
    ggplot(df, aes(x = time, y = errors, color = iteration)) + geom_point()
  )

  Rmisc::multiplot(plotlist = plots, cols = 2)
}

sweeper.plot.sweepables <- function(df) {
  g <- ggplot(df, aes(x = time, y = sweepables, color = iteration)) +
    geom_point() +
    ylab('Num of sweepables processed')
  plot(g)
}
