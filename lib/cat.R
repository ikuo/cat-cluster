cat.files <- function(dir) {
  list.files(dir, pattern = "catcluster.*.profile.log", recursive = TRUE, full.names = TRUE)
}

cat.load.one <- function(file) {
  fread(
    file,
    col.names = c('time', 'member', 'remember.entities', 'num.entities', 'mem.used.redis', 'mem.used', 'mem.total', 'mem.free', 'mem.max')
  ) %>%
    mutate(
      time = parse_date_time2(time, "%Y-%m-%d %H:%M:%S"),
      member = factor(member)
    )
}

cat.load <- function(dir) {
  rbindlist(lapply(cat.files(dir), cat.load.one))
}

cat.bases <- function(df, start = NULL, end = NULL) {
  if (!is.null(start)) { df <- df %>% filter(as.POSIXct(start, tz = "UTC") <= time) }
  if (!is.null(end))   { df <- df %>% filter(as.POSIXct(end, tz = "UTC") >= time) }
  df %>% filter(num.entities > 100) %>%
    mutate(base_time = floor(as.numeric(time) / 5)) %>%
    group_by(base_time) %>%
    summarize(
      n = n(),
      num.entities = mean(num.entities),
      mem.used = sum(mem.used)
    ) %>%
    filter(n - lag(n, order_by = base_time) >= 0)
}

cat.overview <- function(df = NULL, df.bases = NULL) {
  #g <- ggplot(df, aes(x = num.entities, y = mem.used.redis, color = member)) + geom_point() + geom_line()
  #g <- ggplot(df, aes(x = time, y = mem.used, color = member)) + geom_point() + geom_line()
  #g <- ggplot(df, aes(x = num.entities, y = mem.used, color = member)) + geom_point()
  #g <- ggplot(df, aes(x = time, y = num.entities, color = member)) + geom_point()
  #g <- ggplot(df_, aes(x = time, y = mem.used, color = member)) + geom_point()
  g <- ggplot(df.bases, aes(x = base_time, y = mem.used)) + geom_point()
  #g <- ggplot(df, aes(x = base_time, y = n)) + geom_point()
  #g <- ggplot(df, aes(x = num.entities, y = mem.used)) + geom_point()
  plot(g)
}
