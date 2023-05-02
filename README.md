# Search Engine Final Work Skillbox

This is the final project for the "Java Developer from Scratch" course offered by Skillbox. The task is to implement a search engine for a website - an application that allows indexing of pages and quick searching of them.

The search engine should be a Spring application (JAR file) that runs on any server or computer and works with a locally installed MySQL database. It should have a simple web interface and an API through which it can be managed and search results can be obtained.

Principles of the search engine operation:

- Before starting the application, the addresses of the sites to be searched are set in the configuration file.
- The search engine should independently traverse all pages of the specified sites and index them (create a so-called index) in such a way that the most relevant pages can be found later for any search query.
- The user sends a request through the search engine's API. A request is a set of words that need to be found on the website's pages.
- The request is transformed into a list of words translated into their base form in a specific way.
- The index is searched for pages that contain all of these words.
- The search results are ranked, sorted and returned to the user.