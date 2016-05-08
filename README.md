# movie ratings demo API

This API is intended for use by a soon-to-be-delivered Ionic mobile application.

+[![Stories in Ready](https://badge.waffle.io/joshdurbin/movie-ratings-demo.png?label=ready&title=Ready)](https://waffle.io/joshdurbin/movie-ratings-demo)

The API has two sets of endpoints. Endpoints that require authentication and those that don't.

The list of unrestricted endpoints includes:

* Registration with a `POST` to `/register` with a JSON payload of `{'username':'', 'password':'', 'emailAddress':'', 'name':''}` and, if successful, returns an Authentication Header with the Bearer schema containing a JWT.
* Login with a `POST` to `/login` with a JSON payload of `{'username':'', 'password':''}` and, if successful, returns an Authentication Header with the Bearer schema containing a JWT.
* Get All Movies with a `GET` to `/api/movies`
* Get a particular Movie with a `GET` to `/api/movie/$id`
* Search for Movies with a `GET` to `/api/movies/search` with a query parameter `q` containing the search terms
* Get the individual ratings, comments, and user names for a Movie with a `GET` to `/api/movie/$id/ratings`

The list of restricted endpoints includes:

* Creation of a movie with a `POST` to `/api/movie` with a JSON payload of `{'name':'', 'description':'', 'imageURI':''}`. The API will redirect the caller to the individual resource once it's created... ex: `/api/movie/$id`
* Removal of a movie and all ratings with a `DELETE` to `/api/movie/$id`
* Creation of a rating with a `POST` to `/api/movie/$id/rating` with a JSON payload of `{'rating':'', 'comment':''}`. A rating is defined as an integer ranging from 0-10.

[![Deploy](https://www.herokucdn.com/deploy/button.png)](https://heroku.com/deploy?template=https://github.com/joshdurbin/movie-ratings-demo)