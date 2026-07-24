"""
Load generator for the Snip URL shortener, modeled after the traffic
patterns used by Google Cloud's microservices-demo loadgenerator:
https://github.com/GoogleCloudPlatform/microservices-demo/blob/main/src/loadgenerator/locustfile.py

Simulated users mostly browse the homepage and the recent-links table,
occasionally shorten a new URL, and occasionally click through a
shortened link (exercising the redirector service and its Redis/Postgres
lookups). A small fraction of visits hit unknown short codes to produce
realistic 404s.

Run it against the shortener service, e.g.:

    locust -f locustfile.py --host http://localhost:3000
"""

import random
import string
from collections import deque

from locust import HttpUser, task, between

# URLs that get "shortened" during the load test. extractMetadata() on the
# server performs a real fetch of these pages to scrape a title/description,
# so they're picked from fast, reliable, real-world sites to keep that step
# from timing out.
URLS_TO_SHORTEN = [
    "https://example.com", "https://en.wikipedia.org/wiki/Special:Random", "https://www.wikipedia.org", "https://github.com/dash0hq",
    "https://opentelemetry.io", "https://opentelemetry.io/docs/languages/js/", "https://nodejs.org/en", "https://expressjs.com",
    "https://www.postgresql.org", "https://redis.io", "https://developer.mozilla.org/en-US/", "https://www.dash0.com",
    "https://www.dash0.com/guides", "https://news.ycombinator.com", "https://www.iana.org/help/example-domains", "https://www.google.com",
    "https://www.youtube.com", "https://www.facebook.com", "https://www.instagram.com", "https://chatgpt.com", "https://www.reddit.com",
    "https://www.wikipedia.org", "https://x.com", "https://www.whatsapp.com", "https://www.tiktok.com", "https://www.amazon.com",
    "https://www.yahoo.com", "https://www.bing.com", "https://duckduckgo.com", "https://www.yahoo.co.jp", "https://weather.com",
    "https://www.linkedin.com", "https://www.netflix.com", "https://www.pinterest.com", "https://www.microsoft.com", "https://www.twitch.tv",
    "https://vk.com", "https://www.canva.com", "https://claude.ai", "https://www.fandom.com", "https://www.globo.com", "https://mail.ru",
    "https://www.samsung.com", "https://t.me", "https://github.com", "https://www.spotify.com", "https://www.nytimes.com", "https://www.msn.com",
    "https://www.imdb.com", "https://www.paypal.com", "https://www.apple.com", "https://www.roblox.com", "https://www.aliexpress.com",
    "https://openai.com", "https://www.dailymotion.com", "https://www.ebay.com", "https://www.walmart.com", "https://telegram.org",
    "https://www.office.com", "https://www.booking.com", "https://www.bbc.com", "https://www.bbc.co.uk", "https://www.cnn.com",
    "https://www.bloomberg.com", "https://www.reuters.com", "https://www.theguardian.com", "https://www.forbes.com", "https://www.huffpost.com",
    "https://www.washingtonpost.com", "https://www.cnbc.com", "https://www.foxnews.com", "https://www.nationalgeographic.com",
    "https://www.nasa.gov", "https://www.nih.gov", "https://www.cdc.gov", "https://www.who.int", "https://www.imdb.com",
    "https://www.rottentomatoes.com", "https://www.metacritic.com", "https://www.ign.com", "https://www.gamespot.com", "https://www.techcrunch.com",
    "https://www.theverge.com", "https://www.wired.com", "https://www.engadget.com", "https://www.gizmodo.com", "https://www.cnet.com",
    "https://www.zdnet.com", "https://stackoverflow.com", "https://www.quora.com", "https://www.medium.com", "https://www.vimeo.com",
    "https://www.soundcloud.com", "https://www.bandcamp.com", "https://www.deviantart.com", "https://www.behance.net", "https://www.dribbble.com",
    "https://www.flickr.com", "https://www.imgur.com", "https://www.giphy.com", "https://www.unsplash.com", "https://www.pixabay.com",
    "https://www.shutterstock.com", "https://www.etsy.com", "https://www.target.com", "https://www.bestbuy.com", "https://www.homedepot.com",
    "https://www.ikea.com", "https://www.nike.com", "https://www.adidas.com", "https://www.tripadvisor.com", "https://www.airbnb.com",
    "https://www.expedia.com", "https://www.salesforce.com", "https://www.zoom.us"
]


# Public-looking IPs (real allocated ranges) sent as X-Forwarded-For so the
# redirector's ip-api.com geolocation lookup returns varied countries/cities
# instead of "Unknown" for every visit.
SAMPLE_PUBLIC_IPS = [
    "8.8.8.8",         # US
    "1.1.1.1",         # AU/US anycast
    "9.9.9.9",         # CH
    "185.28.101.15",   # UK-ish RIPE block
    "203.0.113.0",     # documentation block, mostly resolves to Unknown
    "104.16.132.229",  # US (Cloudflare)
    "141.101.120.15",  # EU (Cloudflare)
    "45.79.112.203",   # US (Linode)
    "139.162.0.0",     # JP (Linode)
    "129.0.0.0",       # BR-ish ARIN block
]

USER_AGENTS = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15",
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (Android 14; Mobile; rv:126.0) Gecko/126.0 Firefox/126.0",
]

# Short codes created during the run, shared across simulated users so that
# "clicking a shortened link" looks like real traffic on links other users
# created. Bounded so memory stays flat on long-running tests.
created_short_codes = deque(maxlen=500)


def random_short_code(length=8):
    alphabet = string.ascii_letters + string.digits + "_-"
    return "".join(random.choice(alphabet) for _ in range(length))


class SnipUser(HttpUser):
    wait_time = between(1, 10)

    def on_start(self):
        self.index()

    @task(10)
    def index(self):
        """Load the homepage, which itself fetches /api/urls."""
        self.client.get("/", name="/")
        self.client.get("/api/urls", name="/api/urls")

    @task(6)
    def browse_recent_urls(self):
        """A user hitting refresh on the recent-links table."""
        self.client.get("/api/urls", name="/api/urls")

    @task(4)
    def shorten_url(self):
        url = random.choice(URLS_TO_SHORTEN)

        with self.client.post(
            "/api/shorten",
            json={"url": url},
            name="/api/shorten",
            catch_response=True,
        ) as response:
            if response.status_code == 201:
                try:
                    short_code = response.json()["short_code"]
                    created_short_codes.append(short_code)
                except (ValueError, KeyError):
                    response.failure("Malformed /api/shorten response")
            else:
                response.failure(f"Unexpected status {response.status_code}")

    @task(8)
    def visit_short_url(self):
        """Follow a previously created short link, like a real visitor
        clicking a shared Snip URL."""
        if not created_short_codes:
            # Nobody has shortened anything yet this run; prime one.
            self.shorten_url()
            if not created_short_codes:
                return

        short_code = random.choice(created_short_codes)
        headers = {
            "User-Agent": random.choice(USER_AGENTS),
            "X-Forwarded-For": random.choice(SAMPLE_PUBLIC_IPS),
        }

        self.client.get(
            f"/{short_code}",
            name="/[short_code]",
            headers=headers,
            allow_redirects=False,
        )

    @task(1)
    def visit_unknown_short_url(self):
        """Simulate a mistyped or expired short link (expect a 404)."""
        with self.client.get(
            f"/{random_short_code()}",
            name="/[short_code] (unknown)",
            headers={"User-Agent": random.choice(USER_AGENTS)},
            allow_redirects=False,
            catch_response=True,
        ) as response:
            if response.status_code == 404:
                response.success()
            else:
                response.failure(f"Expected 404, got {response.status_code}")
