cat.files <- function(dir) {
  list.files(dir, pattern = "catcluster.*.profile.log", recursive = TRUE, full.names = TRUE)
}

cat.load.one <- function(file) {
  fread(
    file,
    col.names = c('time', 'member', 'remember.entities', 'entities', 'mem.used.redis', 'mem.used', 'mem.total', 'mem.free', 'mem.max')
  ) %>%
    mutate(
      time = parse_date_time2(time, "%Y-%m-%d %H:%M:%S"),
      member = factor(member)
    )
}

cat.load <- function(dir) {
  rbindlist(lapply(cat.files(dir), cat.load.one))
}

cat.bases.raw <- function(df, start = NULL, end = NULL, entities.min = 1e+5) {
  if (!is.null(start)) { df <- df %>% filter(as.POSIXct(start, tz = "UTC") <= time) }
  if (!is.null(end))   { df <- df %>% filter(as.POSIXct(end, tz = "UTC") >= time) }
  df %>% filter(entities > entities.min) %>%
    mutate(base_time = floor(as.numeric(time) / 5)) %>%
    group_by(base_time) %>%
    summarize(
      n = n(),
      entities = quantile(entities, .95),
      mem.used = sum(mem.used),
      mem.used.redis = median(mem.used.redis)
    )
}

cat.bases.filter <- function(df) {
  df <- df %>% filter(n == lag(n, order_by = base_time))
  df %>% filter(n <= quantile(df$n, .95))
}

cat.bases <- function(df) {
  cat.bases.filter(cat.bases.raw(df))
}

cat.overview <- function(df = NULL, df.bases = NULL) {
  plots <- list(
    ggplot(df, aes(x = time, y = entities, color = member)) + geom_point(),
    ggplot(df, aes(x = time, y = mem.used, color = member)) + geom_point() + geom_line(),
    ggplot(df, aes(x = entities, y = mem.used, color = member)) + geom_point(),
    ggplot(df, aes(x = entities, y = mem.used.redis, color = member)) + geom_point(),

    ggplot(df.bases, aes(x = base_time, y = n)) + geom_point(),
    ggplot(df.bases, aes(x = base_time, y = mem.used)) + geom_point(),
    ggplot(df.bases, aes(x = entities, y = mem.used)) + geom_point() +
      xlim(0, max(df.bases$entities)) + ylim(0, max(df.bases$mem.used)),
    ggplot(df.bases, aes(x = entities, y = mem.used.redis)) + geom_point() +
      xlim(0, max(df.bases$entities)) + ylim(0, max(df.bases$mem.used.redis))
  )

  Rmisc::multiplot(plotlist = plots, cols = 2)
}

cat.plot.mem <- function(df.bases) {
  g <- ggplot(df.bases, aes(x = entities, y = mem.used / 1e+3)) + geom_point() +
      xlim(0, max(df.bases$entities)) + ylim(0, max(df.bases$mem.used / 1e+3)) +
      geom_smooth(method = "lm") +
      xlab('Num of Actors') +
      ylab('Total Memory Usage [GB]')
  plot(g)
}
