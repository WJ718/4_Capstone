const Sequelize = require('sequelize');

module.exports = class Record extends Sequelize.Model {
    static init(sequelize) {
        return super.init({
            date : {
                type: Sequelize.DATE,
                allowNull: false,
                primaryKey: true,
            },
            email: {
                type: Sequelize.STRING(100),
                allowNull: false,
            },
            eyesize: {
                type: Sequelize.FLOAT,
                allowNull: false,
            },
            issleep: {
                type: Sequelize.BOOLEAN,
                allowNull: false,
                defaultValue: false,
            },
        }, {
            sequelize,
            timestamps: true,
            underscored: false,
            modelName: 'Record',
            tableName: 'records',
            paranoid: true,
            charset: 'utf8mb4',
            collate: 'utf8mb4_general_ci',
        });
    }

    static associate(db) {
        // TODO : 관계설정
        
    }
}