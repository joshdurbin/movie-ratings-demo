# movie ratings demo API

This API backs the Ionic v2-based mobile app developed [here](https://github.com/joshdurbin/movie-ratings-demo-mobile).

[![Stories in Ready](https://badge.waffle.io/joshdurbin/movie-ratings-demo.png?label=ready&title=Ready)](https://waffle.io/joshdurbin/movie-ratings-demo)

The API has two sets of endpoints. Endpoints that require authentication and those that don't.

The list of unrestricted endpoints includes:

* Registration with a `POST` to `/register` with a JSON payload of `{'username':'', 'password':'', 'emailAddress':'', 'name':''}` and, if successful, returns an Authentication Header with the Bearer schema containing a JWT.
* Login with a `POST` to `/login` with a JSON payload of `{'username':'', 'password':''}` and, if successful, returns an Authentication Header with the Bearer schema containing a JWT.
* Get All Movies with a `GET` to [`/api/movies`](https://movie-ratings-demo.herokuapp.com/api/movies)
* Get a particular Movie with a `GET` to `/api/movies/$id`
* Search for Movies with a `GET` to `/api/movies/search` with a query parameter `q` containing the search terms, example: [`/api/movies/search?q=knight`]

The list of restricted endpoints includes:

* Creation of a movie with a `POST` to `/api/movies` with a query parameter key of `imdbId` and a value of an existing IMDB Movie ID
* Removal of a movie and all ratings with a `DELETE` to `/api/movies/$id`
* Creation of a rating with a `POST` to `/api/movies/$id/ratings` with a JSON payload of `{'rating': 1}`. A rating is defined as an integer ranging from 1-10.

[![Deploy](https://www.herokucdn.com/deploy/button.png)](https://heroku.com/deploy?template=https://github.com/joshdurbin/movie-ratings-demo)