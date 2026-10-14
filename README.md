Simple java based CLI tool to keep track of new releases of book series.

It loads series data from [Hardcover](https://hardcover.app) (or alternatively scrapes it from GoodReads) and shows books released lately or to be released soon.

Currently only windows executables are available.

## Metadata source

By default, series data is loaded from the [Hardcover API](https://docs.hardcover.app/api/getting-started/).
Create an API token on https://hardcover.app/account/api and set it in the `HARDCOVER_API_TOKEN` environment variable.

Series can be added by their Hardcover URL (`https://hardcover.app/series/...`) or their Goodreads URL.
Series that were added by Goodreads URL are looked up on Hardcover by title and author on their next update.

To scrape GoodReads instead (requires Chrome), set the environment variable `BOOK_SERIES_SOURCE=goodreads`.
