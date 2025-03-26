// models/user.js
const Sequelize = require('sequelize');

module.exports = class User extends Sequelize.Model {
    static init(sequelize) {
        return super.init({
            email: {
                type: Sequelize.STRING(100),
                allowNull: false,
                unique: true,
                primaryKey: true,
            },
            password: {
                type: Sequelize.STRING(120),
                allowNull: false,
            }
        }, {
            sequelize,
            timestamps: true,
            underscored: false,
            modelName: 'User',
            tableName: 'users',
            paranoid: true,
            charset: 'utf8mb4',
            collate: 'utf8mb4_general_ci',
        });
    }

    static associate(db) {
        db.User.hasMany(db.Schedule, { foreignKey: 'email', sourceKey: 'email', onDelete: 'CASCADE' });
    }
};
