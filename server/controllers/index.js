exports.renderMain = async (req, res, next) => {
    try {
      res.render('main');
    } catch (error) {
      console.error(error);
      next(error);
    }
  };