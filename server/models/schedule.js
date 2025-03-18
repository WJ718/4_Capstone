const Sequelize = require('sequelize');

module.exports = class Schedule extends Sequelize.Model {
    static init(sequelize) {
        return super.init({
            date: {
                type:Sequelize.DATE,
                allowNull:false,
                primaryKey: true,
            },
            email: {
                type:Sequelize.STRING(100),
                allowNull:false,
            },
            content: {
                type:Sequelize.STRING(100),
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
        // TODO: 관계설정
        
    }
}