const Sequelize = require('sequelize');

module.exports = class Schedule extends Sequelize.Model {
    static init(sequelize) {
        return super.init({
            schedule_id: {  
                type: Sequelize.INTEGER,
                autoIncrement: true,
                primaryKey: true,
            },
            email: {
                type: Sequelize.STRING(100),
                allowNull: false,
            },
            date: {
                type: Sequelize.DATE,
                allowNull: false,
            },
            memo: {  
                type: Sequelize.STRING(255),
                allowNull: false,
                defaultValue: " ",
            },
        }, {
            sequelize,
            timestamps: true,
            underscored: false,
            modelName: 'Schedule',
            tableName: 'schedules',
            paranoid: true,
            charset: 'utf8mb4',
            collate: 'utf8mb4_general_ci',
        });
    }

    static associate(db) {
        db.Schedule.belongsTo(db.User, { foreignKey: 'email', targetKey: 'email', onDelete: 'CASCADE' });
    }
};
